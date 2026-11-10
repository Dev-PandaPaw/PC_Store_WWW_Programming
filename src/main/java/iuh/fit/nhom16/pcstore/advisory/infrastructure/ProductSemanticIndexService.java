package iuh.fit.nhom16.pcstore.advisory.infrastructure;

import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Idempotent derived-index synchronization. The database remains the source of truth. */
@Service
@ConditionalOnProperty(name="app.recommendations.enabled",havingValue="true")
public class ProductSemanticIndexService {
    public record SyncResult(int updated,int removed,boolean busy) {}
    private final RecommendationCatalog catalog;
    private final VectorStore vectors;
    private final JdbcTemplate jdbc;
    private final String signature;
    public ProductSemanticIndexService(RecommendationCatalog catalog, VectorStore vectors, JdbcTemplate jdbc,
            @Value("${app.ai.vector.model:gemini-embedding-001}") String model) {
        this.catalog=catalog; this.vectors=vectors; this.jdbc=jdbc; this.signature=model+":768:semantic-v1";
    }
    @Transactional
    public SyncResult synchronize() {
        // Transaction-scoped lock also serializes the scheduler and admin calls across app instances.
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT pg_try_advisory_xact_lock(71616001)",Boolean.class)))
            return new SyncResult(0,0,true);
        Map<String,String> indexed=new HashMap<>();
        jdbc.query("SELECT id::text,metadata->>'contentHash',metadata->>'embeddingSignature' FROM product_semantic_vectors", rs -> {
            indexed.put(rs.getString(1),rs.getString(2)+":"+rs.getString(3));
        });
        Set<String> active=new HashSet<>();
        List<Document> changed=new ArrayList<>();
        for (var item:catalog.indexableProducts()) {
            String id=item.id().toString(); active.add(id);
            String content=item.product().searchable()+". Danh mục: "+item.product().category()
                    +". Giá VND: "+item.product().price()+". Socket: "+Objects.toString(item.socket(),"unknown");
            String hash=hash(content);
            if ((hash+":"+signature).equals(indexed.get(id))) continue;
            changed.add(Document.builder().id(id).text(content).metadata(Map.of("productId",id,
                    "category",item.product().category(),"embeddingSignature",signature,"contentHash",hash)).build());
        }
        // Small batches keep provider payloads bounded; failures roll back the complete database update.
        for (int offset=0;offset<changed.size();offset+=20)
            vectors.add(changed.subList(offset,Math.min(changed.size(),offset+20)));
        List<String> stale=indexed.keySet().stream().filter(id -> !active.contains(id)).toList();
        if (!stale.isEmpty()) vectors.delete(stale);
        return new SyncResult(changed.size(),stale.size(),false);
    }
    public int indexedCount() {
        Integer count=jdbc.queryForObject("SELECT count(*) FROM product_semantic_vectors",Integer.class);
        return count==null ? 0 : count;
    }
    private static String hash(String content) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
