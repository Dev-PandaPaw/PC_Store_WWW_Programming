package iuh.fit.nhom16.pcstore.ai.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

/** SimpleVectorStore trong RAM, đổi snapshot chỉ sau khi toàn bộ ingestion thành công. */
public final class ProductVectorStore implements VectorStore {
    private final EmbeddingModel embeddingModel;
    private volatile SimpleVectorStore current;

    ProductVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.current = SimpleVectorStore.builder(embeddingModel).build();
    }

    synchronized void replaceAll(List<Document> documents) {
        var staging = SimpleVectorStore.builder(embeddingModel).build();
        if (!documents.isEmpty()) staging.add(documents);
        current = staging; // Publish nguyên khối; lỗi ở trên không làm mất dữ liệu cũ.
    }

    @Override
    public synchronized void add(List<Document> documents) { current.add(documents); }

    @Override
    public synchronized void delete(List<String> ids) { current.delete(ids); }

    @Override
    public synchronized void delete(Filter.Expression expression) { current.delete(expression); }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return current.similaritySearch(request);
    }
}
