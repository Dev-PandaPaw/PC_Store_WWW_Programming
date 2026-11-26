package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/** Admin inspection of embeddings already stored in memory or PgVector. No embedding calls. */
@Service
public class ProductVectorInspectionService {
    public record DocumentPage(String storage, long total, int offset, int limit,
                               List<ProductVectorStore.VectorDocument> documents) {}
    private final ProductVectorStore store;
    private final ObjectProvider<JdbcTemplate> jdbc;
    private final JsonMapper json;

    public ProductVectorInspectionService(ProductVectorStore store, ObjectProvider<JdbcTemplate> jdbc, JsonMapper json) {
        this.store = store;
        this.jdbc = jdbc;
        this.json = json;
    }

    public DocumentPage documents(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100)
            throw new AiFailure(400, "INVALID_VECTOR_PAGE", "offset phải >= 0; limit từ 1 đến 100.");
        if (!store.isPersistent()) {
            var documents = store.memoryDocuments();
            return new DocumentPage("memory", documents.size(), offset, limit,
                    documents.stream().skip(offset).limit(limit).toList());
        }
        try {
            JdbcTemplate database = jdbc.getIfAvailable();
            if (database == null) throw new AiFailure(503, "VECTOR_INSPECTION_UNAVAILABLE", "Chưa kết nối PgVector.");
            Long total = database.queryForObject("SELECT count(*) FROM product_semantic_vectors", Long.class);
            List<ProductVectorStore.VectorDocument> documents = database.query("""
                    SELECT id::text AS id, content, metadata::text AS metadata, embedding::text AS embedding
                    FROM product_semantic_vectors ORDER BY id LIMIT ? OFFSET ?
                    """, (rs, row) -> new ProductVectorStore.VectorDocument(
                    rs.getString("id"), rs.getString("content"),
                    json.readValue(rs.getString("metadata"), new TypeReference<Map<String, Object>>() {}),
                    json.readValue(rs.getString("embedding"), new TypeReference<List<Float>>() {}), 0), limit, offset);
            return new DocumentPage("pgvector", total == null ? 0 : total, offset, limit, documents);
        } catch (DataAccessException error) {
            throw new AiFailure(503, "VECTOR_INSPECTION_UNAVAILABLE", "Chưa đọc được dữ liệu từ PgVector.");
        }
    }
}
