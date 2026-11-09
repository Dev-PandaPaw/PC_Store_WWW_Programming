package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import iuh.fit.nhom16.pcstore.ai.AiSettings;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ProductVectorIngestionService {
    public record ReindexResult(String status, int productCount, int documentCount, Instant completedAt) {}
    private final ProductRepository products;
    private final ProductChunker chunker;
    private final ProductVectorStore vectorStore;
    private final AiSettings settings;
    private final TransactionTemplate readTransaction;
    private final ReentrantLock reindexLock = new ReentrantLock();

    public ProductVectorIngestionService(ProductRepository products, ProductChunker chunker,
            ProductVectorStore vectorStore, AiSettings settings, PlatformTransactionManager transactionManager) {
        this.products = products;
        this.chunker = chunker;
        this.vectorStore = vectorStore;
        this.settings = settings;
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
    }

    public ReindexResult reindexAll() {
        if (!settings.configured())
            throw new AiFailure(503, "VECTOR_NOT_CONFIGURED", "Chưa cấu hình GEMINI_API_KEY cho embedding.");
        if (!reindexLock.tryLock())
            throw new AiFailure(409, "VECTOR_REINDEX_IN_PROGRESS", "Đang đồng bộ vector. Hãy đợi lượt hiện tại hoàn tất.");
        try {
            // Extract + Transform trong transaction ngắn; không giữ DB connection khi gọi Gemini.
            var snapshot = Objects.requireNonNull(readTransaction.execute(transaction -> {
                var active = products.findAll().stream().filter(p -> "ACTIVE".equals(p.getStatus())).toList();
                var documents = active.stream().flatMap(p -> chunker.chunk(p).stream()).toList();
                return new CatalogSnapshot(active.size(), documents);
            }));
            // Embed + Load vào store tạm, sau đó thay thế toàn bộ store đang phục vụ.
            vectorStore.replaceAll(snapshot.documents());
            return new ReindexResult("SUCCESS", snapshot.productCount(), snapshot.documents().size(), Instant.now());
        } finally {
            reindexLock.unlock();
        }
    }

    private record CatalogSnapshot(int productCount, java.util.List<org.springframework.ai.document.Document> documents) {}
}
