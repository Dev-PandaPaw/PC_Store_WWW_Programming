package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static iuh.fit.nhom16.pcstore.pcbuild.PcBuildDtos.*;
import static iuh.fit.nhom16.pcstore.pcbuild.PcBuildInventory.conflict;

@Service
public class PcBuildService {
    private static final String PROPOSALS = PcBuildService.class.getName() + ".proposals";
    private final CartStorageService storage;
    private final CartService carts;
    private final PcBuildClient python;
    private final PcBuildInventory inventory;
    public PcBuildService(CartStorageService storage, CartService carts, PcBuildClient python, PcBuildInventory inventory) {
        this.storage = storage; this.carts = carts; this.python = python; this.inventory = inventory;
    }
    public AdviceView advice(HttpSession session, UUID owner, AdviceInput input) {
        CartSession snapshot = storage.snapshot(session, owner);
        Set<UUID> removed = new LinkedHashSet<>(input.replaceProductIds() == null ? List.of() : input.replaceProductIds());
        if (removed.size() != (input.replaceProductIds() == null ? 0 : input.replaceProductIds().size())
                || !snapshot.quantities().keySet().containsAll(removed))
            throw conflict("PC_BUILD_INVALID_REPLACEMENT", "Món cần thay phải có trong giỏ hiện tại.");
        var kept = new LinkedHashMap<>(snapshot.quantities());
        removed.forEach(kept::remove);
        Map<String,Object> request = new LinkedHashMap<>();
        request.put("question", input.question());
        request.put("budget_vnd", input.budgetVnd().toPlainString());
        request.put("require_dedicated_gpu", input.requireDedicatedGpu());
        request.put("include_rag", input.includeRag());
        request.put("allow_catalog_capacity", true);
        request.put("diversify", true);
        request.put("use_snapshot", true);
        request.put("max_search_ms", 1000);
        request.put("prefer_benchmarks", true);
        request.put("evidence_strategy", "indexed");
        request.put("workload", Boolean.TRUE.equals(input.requireDedicatedGpu()) ? "gaming" : "general");
        Map<String,Object> requirements = new LinkedHashMap<>();
        requirements.put("require_dedicated_gpu", input.requireDedicatedGpu());
        if (input.minRamGb() != null) { request.put("min_ram_gb", input.minRamGb()); requirements.put("min_ram_gb", input.minRamGb()); }
        if (input.minStorageGb() != null) { request.put("min_storage_gb", input.minStorageGb()); requirements.put("min_storage_gb", input.minStorageGb()); }
        request.put("cart_id", snapshot.mergeId().toString());
        request.put("cart_snapshot_version", snapshot.version());
        request.put("cart_items", items(kept));
        // No database or session lock is held during the potentially slow RAG call.
        Map<String,Object> response = python.post("/api/pc-build/advice", request);
        if (!snapshot.mergeId().toString().equals(String.valueOf(response.get("cart_id")))
                || number(response.get("cart_snapshot_version")) != snapshot.version()) throw invalidResponse();
        Map<String,Object> search = map(response.get("build_search"));
        List<ProposalView> views = new ArrayList<>();
        List<Proposal> prepared = new ArrayList<>();
        for (String group : List.of("recommendations", "provisional_builds")) {
            List<?> options = list(search.get(group));
            for (int index = 0; index < Math.min(options.size(), 5); index++) {
                var option = map(options.get(index));
                var compatibility = map(option.get("compatibility"));
                String status = String.valueOf(compatibility.get("status"));
                if (!Set.of("compatible", "needs_verification").contains(status)) throw invalidResponse();
                if (group.equals("recommendations") && (!status.equals("compatible")
                        || !Boolean.TRUE.equals(compatibility.get("complete")))) throw invalidResponse();
                List<Line> lines = new ArrayList<>();
                Map<UUID,Integer> target = new LinkedHashMap<>();
                BigDecimal total = BigDecimal.ZERO;
                for (Object raw : list(option.get("items"))) {
                    var item = map(raw);
                    UUID id = UUID.fromString(String.valueOf(item.get("product_id")));
                    int quantity = Math.toIntExact(number(item.get("quantity")));
                    BigDecimal price = decimal(item.get("unit_price_vnd"));
                    if (quantity < 1 || quantity > CartService.MAX_QUANTITY_PER_SKU || price.signum() < 0
                            || target.put(id, quantity) != null) throw invalidResponse();
                    BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
                    if (lineTotal.compareTo(decimal(item.get("line_total_vnd"))) != 0) throw invalidResponse();
                    total = total.add(lineTotal);
                    lines.add(new Line(id, String.valueOf(item.get("name")), quantity, price,
                            number(item.get("product_version")), kept.containsKey(id)));
                }
                if (target.isEmpty() || target.size() > CartService.MAX_DISTINCT_SKUS
                        || !kept.entrySet().stream().allMatch(e -> Objects.equals(target.get(e.getKey()), e.getValue()))
                        || total.compareTo(decimal(option.get("subtotal_vnd"))) != 0
                        || total.compareTo(input.budgetVnd()) > 0) throw invalidResponse();
                UUID id = UUID.randomUUID();
                Instant expires = Instant.now().plusSeconds(600);
                var proposal = new Proposal(owner, snapshot.mergeId(), snapshot.version(), Map.copyOf(snapshot.quantities()),
                        List.copyOf(lines), Map.copyOf(target), expires, Map.copyOf(requirements));
                prepared.add(proposal);
                views.add(new ProposalView(id, group, index, expires, List.copyOf(lines),
                        snapshot.quantities().keySet().stream().filter(pid -> !target.containsKey(pid)).toList(),
                        total, !status.equals("compatible") || !Boolean.TRUE.equals(compatibility.get("complete"))));
            }
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            // Replace the last batch, bounded by the response limit and bound to this session/owner.
            var saved = new LinkedHashMap<UUID,Proposal>();
            for (int i = 0; i < views.size(); i++) saved.put(views.get(i).proposalId(), prepared.get(i));
            session.setAttribute(PROPOSALS, saved);
        }
        return new AdviceView(response, List.copyOf(views), snapshot.version());
    }
    public CartResponse confirm(HttpSession session, UUID owner, ConfirmInput input) {
        if (!Boolean.TRUE.equals(input.confirmed())) throw conflict("PC_BUILD_CONFIRMATION_REQUIRED", "Cần xác nhận thay đổi giỏ.");
        Proposal proposal;
        synchronized (WebUtils.getSessionMutex(session)) {
            proposal = proposals(session).get(input.proposalId());
        }
        if (proposal == null || !Objects.equals(proposal.owner(), owner) || !proposal.expires().isAfter(Instant.now()))
            throw conflict("PC_BUILD_PROPOSAL_EXPIRED", "Đề xuất hết hạn hoặc không thuộc phiên này. Hãy lấy đề xuất mới.");
        CartSession before = storage.snapshot(session, owner);
        validateCart(proposal, before);
        // Recheck technical data without invoking DeepSeek, outside write locks.
        Map<String,Object> checked = python.post("/api/internal/cart/check", Map.of(
                "cart_id", before.mergeId().toString(), "cart_snapshot_version", before.version(),
                "cart_items", items(proposal.target())));
        var report = map(checked.get("compatibility"));
        String compatibility = String.valueOf(report.get("status"));
        if (!Boolean.TRUE.equals(checked.get("purchasable")) || compatibility.equals("incompatible"))
            throw conflict("PC_BUILD_RECHECK_FAILED", "Cấu hình hoặc tồn kho đã thay đổi. Hãy lấy đề xuất mới.");
        if (!Set.of("compatible", "needs_verification").contains(compatibility)) throw invalidResponse();
        CartResponse result = storage.executeChecked(session, owner, cart -> {
            validateCart(proposal, cart);
            inventory.validateLocked(proposal.lines());
            long originalVersion = cart.version();
            boolean changedRequirements = !cart.buildRequirements().equals(proposal.requirements());
            CartResponse applied = carts.applyProposal(cart, proposal.target(), proposal.cartVersion());
            cart.setBuildRequirements(proposal.requirements());
            if (changedRequirements && cart.version() == originalVersion) {
                cart.incrementVersion();
                return carts.get(cart);
            }
            return applied;
        });
        synchronized (WebUtils.getSessionMutex(session)) { proposals(session).remove(input.proposalId()); }
        return result;
    }
    static void validateCart(Proposal proposal, CartSession cart) {
        if (!proposal.expires().isAfter(Instant.now()))
            throw conflict("PC_BUILD_PROPOSAL_EXPIRED", "Đề xuất đã hết hạn. Hãy lấy đề xuất mới.");
        if (!proposal.cartId().equals(cart.mergeId()) || proposal.cartVersion() != cart.version()
                || !proposal.original().equals(cart.quantities()))
            throw conflict("CART_VERSION_CONFLICT", "Giỏ hàng đã thay đổi. Hãy lấy đề xuất mới trước khi xác nhận.");
    }
    private static List<Map<String,Object>> items(Map<UUID,Integer> values) {
        return values.entrySet().stream().map(e -> Map.<String,Object>of("product_id", e.getKey().toString(), "quantity", e.getValue())).toList();
    }
    @SuppressWarnings("unchecked") private static Map<UUID,Proposal> proposals(HttpSession session) {
        Object value = session.getAttribute(PROPOSALS);
        return value instanceof Map<?,?> ? (Map<UUID,Proposal>) value : new LinkedHashMap<>();
    }
    @SuppressWarnings("unchecked") static Map<String,Object> map(Object value) {
        if (!(value instanceof Map<?,?>)) throw invalidResponse();
        return (Map<String,Object>) value;
    }
    static List<?> list(Object value) {
        if (!(value instanceof List<?> values)) throw invalidResponse();
        return values;
    }
    static long number(Object value) {
        try { return new BigDecimal(String.valueOf(value)).longValueExact(); }
        catch (RuntimeException ex) { throw invalidResponse(); }
    }
    static BigDecimal decimal(Object value) {
        try { return new BigDecimal(String.valueOf(value)); }
        catch (RuntimeException ex) { throw invalidResponse(); }
    }
    static CartException invalidResponse() {
        return new CartException(HttpStatus.BAD_GATEWAY, "PC_BUILD_INVALID_RESPONSE", "Dịch vụ tư vấn trả dữ liệu không hợp lệ.");
    }
    record Proposal(UUID owner, UUID cartId, long cartVersion, Map<UUID,Integer> original,
                    List<Line> lines, Map<UUID,Integer> target, Instant expires, Map<String,Object> requirements) implements Serializable {
        Proposal(UUID owner, UUID cartId, long cartVersion, Map<UUID,Integer> original,
                 List<Line> lines, Map<UUID,Integer> target, Instant expires) {
            this(owner, cartId, cartVersion, original, lines, target, expires, Map.of());
        }
    }
}
