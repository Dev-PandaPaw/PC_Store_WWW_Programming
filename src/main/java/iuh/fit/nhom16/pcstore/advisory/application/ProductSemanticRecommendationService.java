package iuh.fit.nhom16.pcstore.advisory.application;

import iuh.fit.nhom16.pcstore.advisory.api.RecentCategoryProvider;
import iuh.fit.nhom16.pcstore.advisory.domain.ComponentCompatibility;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import iuh.fit.nhom16.pcstore.ai.vector.ProductVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

@Service
public class ProductSemanticRecommendationService {
    public static final int TOP_K = 5;
    public static final double MIN_SIMILARITY = 0.70;
    public static final Set<String> CATEGORIES = Set.of("cpu","mainboard","vga","ram","storage","cooling","case","psu","monitor","mouse","keyboard");
    public record Recommendation(StoreProduct product, double similarityScore, String compatibilityStatus) {}
    public record Result(List<Recommendation> products, String status) {}
    private final RecommendationCatalog catalog;
    private final Supplier<VectorStore> vectors;
    private final Supplier<RecentCategoryProvider> behavior;
    private final Clock clock;
    private final String signature;
    private final double priceTolerance;

    @Autowired
    public ProductSemanticRecommendationService(RecommendationCatalog catalog, ObjectProvider<VectorStore> vectors,
            ObjectProvider<RecentCategoryProvider> behavior,
            @Value("${app.ai.vector.model:gemini-embedding-001}") String model,
            @Value("${app.recommendations.price-tolerance:0.20}") double priceTolerance) {
        this(catalog, vectors::getIfAvailable, behavior::getIfAvailable, Clock.systemUTC(), model + ":768:semantic-v1",priceTolerance);
    }
    public ProductSemanticRecommendationService(RecommendationCatalog catalog, Supplier<VectorStore> vectors,
            Supplier<RecentCategoryProvider> behavior, Clock clock, String signature) {
        this(catalog,vectors,behavior,clock,signature,0.20);
    }
    public ProductSemanticRecommendationService(RecommendationCatalog catalog, Supplier<VectorStore> vectors,
            Supplier<RecentCategoryProvider> behavior, Clock clock, String signature,double priceTolerance) {
        if (!Double.isFinite(priceTolerance) || priceTolerance<0 || priceTolerance>1)
            throw new IllegalArgumentException("Price tolerance must be between 0 and 1");
        this.catalog=catalog; this.vectors=vectors; this.behavior=behavior; this.clock=clock; this.signature=signature;
        this.priceTolerance=priceTolerance;
    }
    public Result personalized(UUID userId, List<UUID> selectedProductIds) {
        if (userId == null) throw failure(401,"IDENTITY_REQUIRED","Bạn cần đăng nhập.");
        List<Item> context = context(selectedProductIds);
        RecentCategoryProvider provider = behavior.get();
        if (provider == null) throw failure(503,"BEHAVIOR_UNAVAILABLE","Chưa kết nối dịch vụ lịch sử xem sản phẩm.");
        List<String> recent;
        try { recent=provider.topCategories(userId,clock.instant().minus(Duration.ofDays(30)),3); }
        catch (RuntimeException e) { throw failure(503,"BEHAVIOR_UNAVAILABLE","Chưa đọc được lịch sử xem sản phẩm."); }
        List<String> categories = recent == null ? List.of() : recent.stream().filter(Objects::nonNull)
                .filter(CATEGORIES::contains).distinct().limit(3).toList();
        if (categories.isEmpty()) return new Result(List.of(),"insufficient_history");
        String prompt = "Gợi ý linh kiện máy tính thuộc các danh mục quan tâm gần đây: " + String.join(", ",categories)
                + contextText(context);
        return search(prompt,categories,null,context,null);
    }
    public Result similar(UUID productId, List<UUID> selectedProductIds) {
        Item source = read(Set.of(productId)).get(productId);
        if (source == null || !"ACTIVE".equals(source.status()) || !CATEGORIES.contains(source.product().category()))
            throw failure(404,"PRODUCT_NOT_FOUND","Không tìm thấy linh kiện đang được hiển thị.");
        List<Item> context = context(selectedProductIds);
        // Replacement CPU/mainboard must also preserve the source platform when no build was supplied.
        if (context.isEmpty() && ComponentCompatibility.socketComponent(source)) {
            if (ComponentCompatibility.normalizeSocket(source.socket())==null) return new Result(List.of(),"no_match");
            context=List.of(source);
        }
        String prompt = "Linh kiện thay thế cùng loại, thông số và tầm giá tương tự: " + source.product().searchable()
                + ". Giá tham chiếu VND: " + source.product().price() + contextText(context);
        return search(prompt,List.of(source.product().category()),source,context,source.product().price());
    }
    private Result search(String prompt, List<String> categories, Item source, List<Item> context, Long price) {
        VectorStore store = vectors.get();
        if(store instanceof ProductVectorStore products && !products.isPersistent())
            throw failure(503,"VECTOR_UNAVAILABLE","Bật profile semantic để dùng gợi ý từ PgVector.");
        if (store == null) throw failure(503,"VECTOR_UNAVAILABLE","Tìm kiếm vector chưa được bật.");
        // Categories are whitelisted; metadata never supplies prices, stock or compatibility verdicts.
        String filter = "embeddingSignature == '" + signature + "' && category in ["
                + String.join(",",categories.stream().map(c -> "'"+c+"'").toList()) + "]";
        if (source != null) filter += " && productId != '" + source.id() + "'";
        List<Document> documents;
        try { documents=store.similaritySearch(SearchRequest.builder().query(prompt).topK(TOP_K)
                    .similarityThreshold(MIN_SIMILARITY).filterExpression(filter).build()); }
        catch (RuntimeException e) { throw failure(503,"VECTOR_UNAVAILABLE","Dịch vụ tìm kiếm vector tạm thời gián đoạn."); }
        if (documents == null) throw failure(503,"VECTOR_UNAVAILABLE","Dịch vụ tìm kiếm vector không trả dữ liệu hợp lệ.");
        Map<UUID, Double> scores = new HashMap<>();
        for (Document document : documents) {
            if (document == null || document.getScore() == null) continue;
            double score = document.getScore();
            if (!Double.isFinite(score) || score < MIN_SIMILARITY || score > 1.0) continue;
            try {
                UUID id = UUID.fromString(String.valueOf(document.getMetadata().get("productId")));
                if (source == null || !source.id().equals(id)) scores.merge(id,score,Math::max);
            } catch (IllegalArgumentException ignored) { /* Discard corrupt/stale index metadata. */ }
        }
        // One catalog statement rechecks candidates, source and build after the embedding call.
        Set<UUID> currentIds=new HashSet<>(scores.keySet());
        context.forEach(item -> currentIds.add(item.id()));
        if (source!=null) currentIds.add(source.id());
        Map<UUID, Item> current = read(currentIds);
        if (source != null) {
            Item latest=current.get(source.id());
            if (latest==null || !"ACTIVE".equals(latest.status()) || !latest.product().category().equals(source.product().category()))
                throw failure(404,"PRODUCT_NOT_FOUND","Sản phẩm nguồn không còn được hiển thị.");
            price=latest.product().price();
        }
        if (!context.isEmpty()) context=validateContext(context.stream().map(Item::id).toList(),current);
        List<Recommendation> result = new ArrayList<>();
        for (var entry : scores.entrySet()) {
            Item item=current.get(entry.getKey());
            if (item == null || !item.purchasable() || !categories.contains(item.product().category())) continue;
            if (price != null && (item.product().price() < price*(1-priceTolerance)
                    || item.product().price() > price*(1+priceTolerance))) continue;
            String compatibility=ComponentCompatibility.check(item,context);
            if (compatibility.equals("incompatible")) continue;
            if (ComponentCompatibility.socketComponent(item) && context.stream().anyMatch(ComponentCompatibility::socketComponent)
                    && compatibility.equals("needs_verification")) continue;
            result.add(new Recommendation(item.product(),entry.getValue(),compatibility));
        }
        result.sort(Comparator.comparingDouble(Recommendation::similarityScore).reversed().thenComparing(r -> r.product().id()));
        List<Recommendation> top=result.stream().limit(TOP_K).toList();
        return new Result(top,top.isEmpty() ? "no_match" : "ok");
    }
    private List<Item> context(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        if (ids.size() > 20 || ids.stream().anyMatch(Objects::isNull)) throw failure(400,"INVALID_CONTEXT","Tối đa 20 ID linh kiện hợp lệ.");
        Set<UUID> distinct=new LinkedHashSet<>(ids);
        return validateContext(ids,read(distinct));
    }
    private List<Item> validateContext(List<UUID> ids,Map<UUID, Item> items) {
        List<Item> selected=ids.stream().distinct().map(items::get).toList();
        if (selected.stream().anyMatch(i -> i==null || !"ACTIVE".equals(i.status()) || !CATEGORIES.contains(i.product().category())))
            throw failure(400,"INVALID_CONTEXT","Có linh kiện trong ngữ cảnh không còn được hiển thị.");
        if (!ComponentCompatibility.contextValid(selected))
            throw failure(400,"INVALID_CONTEXT","Socket linh kiện đã chọn chưa xác định hoặc không khớp.");
        return selected;
    }
    private Map<UUID, Item> read(Set<UUID> ids) {
        try { return catalog.findAll(ids); }
        catch (RuntimeException e) { throw failure(503,"CATALOG_UNAVAILABLE","Chưa đọc được danh mục sản phẩm."); }
    }
    private static String contextText(List<Item> context) {
        return context.isEmpty() ? "" : ". Ngữ cảnh linh kiện: " + String.join("; ",context.stream()
                .map(i -> i.product().category()+": "+i.product().name()+", socket="+i.socket()).toList());
    }
    private static RecommendationFailure failure(int status,String code,String message) { return new RecommendationFailure(status,code,message); }
}
