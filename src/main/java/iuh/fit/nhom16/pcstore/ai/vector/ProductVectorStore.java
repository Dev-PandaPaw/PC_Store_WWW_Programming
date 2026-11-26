package iuh.fit.nhom16.pcstore.ai.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** SimpleVectorStore trong RAM, đổi snapshot chỉ sau khi toàn bộ ingestion thành công. */
public final class ProductVectorStore implements VectorStore {
    private final EmbeddingModel embeddingModel;
    private volatile InspectableSimpleVectorStore current;
    private final VectorStore persistent;

    ProductVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.current = new InspectableSimpleVectorStore(embeddingModel);
        this.persistent = null;
    }

    public ProductVectorStore(VectorStore persistent) {
        this.embeddingModel=null; this.persistent=java.util.Objects.requireNonNull(persistent);
    }

    public boolean isPersistent() { return persistent!=null; }
    public int memoryDocumentCount() { return persistent == null ? current.documentCount() : 0; }
    private VectorStore active() { return persistent==null ? current : persistent; }

    synchronized void replaceAll(List<Document> documents) {
        if(persistent!=null) throw new IllegalStateException("Use the persistent catalog synchronizer for PgVector reindexing");
        var staging = new InspectableSimpleVectorStore(embeddingModel);
        if (!documents.isEmpty()) staging.add(documents);
        current = staging; // Publish nguyên khối; lỗi ở trên không làm mất dữ liệu cũ.
    }

    @Override
    public synchronized void add(List<Document> documents) { active().add(documents); }

    @Override
    public synchronized void delete(List<String> ids) { active().delete(ids); }

    @Override
    public synchronized void delete(Filter.Expression expression) { active().delete(expression); }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return active().similaritySearch(request);
    }

    /** Detached, read-only copies of the actual stored embeddings. Never calls the model. */
    List<VectorDocument> memoryDocuments() {
        if (isPersistent()) throw new IllegalStateException("Persistent embeddings must be read from PostgreSQL");
        return current.documents();
    }

    public record VectorDocument(String id, String text, Map<String, Object> metadata,
                                 List<Float> embedding, int dimensions) {
        public VectorDocument {
            metadata = Map.copyOf(metadata);
            embedding = List.copyOf(embedding);
            dimensions = embedding.size();
        }
    }

    private static final class InspectableSimpleVectorStore extends SimpleVectorStore {
        private final JsonMapper json = JsonMapper.builder().build();

        InspectableSimpleVectorStore(EmbeddingModel model) {
            super(SimpleVectorStore.builder(model));
        }

        int documentCount() { return store.size(); }

        List<VectorDocument> documents() {
            // Spring AI's stored-content type is package-private. Map its public JSON
            // properties into our DTO instead of exposing internal mutable float arrays.
            java.util.Collection<?> contents = store.values();
            return contents.stream()
                    .map(content -> json.convertValue(content, StoredContent.class))
                    .map(content -> new VectorDocument(content.id(), content.text(), content.metadata(), content.embedding(), 0))
                    .sorted(java.util.Comparator.comparing(VectorDocument::id)).toList();
        }

        private record StoredContent(String id, String text, Map<String, Object> metadata, List<Float> embedding) {}
    }
}
