package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import iuh.fit.nhom16.pcstore.ai.AiSettings;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductVectorIngestionServiceTest {
    @Test void semanticAdminEndpointUsesThePersistentRecommendationIndexer() {
        var index=mock(iuh.fit.nhom16.pcstore.advisory.infrastructure.ProductSemanticIndexService.class);
        when(index.synchronize()).thenReturn(new iuh.fit.nhom16.pcstore.advisory.infrastructure.ProductSemanticIndexService.SyncResult(2,1,false));
        when(index.indexedCount()).thenReturn(5);
        var persistent=new ProductVectorStore(mock(org.springframework.ai.vectorstore.VectorStore.class));
        var routed=new ProductVectorIngestionService(repository,new ProductChunker(),persistent,settings,transactions,()->index);
        assertEquals(5,routed.reindexAll().documentCount());
        verify(index).synchronize(); verify(index).indexedCount();
        verifyNoInteractions(repository,embedding);
    }
    @Test void semanticAdminReindexRespectsTheSharedDatabaseLock() {
        var index=mock(iuh.fit.nhom16.pcstore.advisory.infrastructure.ProductSemanticIndexService.class);
        when(index.synchronize()).thenReturn(new iuh.fit.nhom16.pcstore.advisory.infrastructure.ProductSemanticIndexService.SyncResult(0,0,true));
        var routed=new ProductVectorIngestionService(repository,new ProductChunker(),store,settings,transactions,()->index);
        assertEquals(409,assertThrows(AiFailure.class,routed::reindexAll).status);
        verify(index,never()).indexedCount();
    }
    final ProductRepository repository = mock(ProductRepository.class);
    final EmbeddingModel embedding = mock(EmbeddingModel.class);
    final ProductVectorStore store = new ProductVectorStore(embedding);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    final AiSettings settings = new AiSettings("test-key", "chat-unused", "https://generativelanguage.googleapis.com", 2000, 20, 100, 1000, false);
    ProductVectorIngestionService service;

    @BeforeEach void setup() {
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(embedding.embed(any(Document.class))).thenReturn(new float[]{1, 0, 0});
        when(embedding.embed(anyString())).thenReturn(new float[]{1, 0, 0});
        service = new ProductVectorIngestionService(repository, new ProductChunker(), store, settings, transactions);
    }

    List<Document> documents() {
        return store.similaritySearch(SearchRequest.builder().query("CPU").topK(100).similarityThreshold(0).build());
    }

    @Test void reindexUpdatesWithoutDuplicatesAndRemovesInactiveOrDeletedProducts() {
        var cpu = ProductChunkerTest.cpu(); var removed = ProductChunkerTest.cpu(); removed.setId(UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of(cpu, removed));
        var first = service.reindexAll();
        assertEquals(2, first.productCount()); assertEquals(2, first.documentCount());
        cpu.setPrice(new java.math.BigDecimal("2990000")); cpu.setStockOnHand(0); removed.setStatus("INACTIVE");
        var second = service.reindexAll();
        assertEquals("SUCCESS", second.status()); assertEquals(1, second.productCount());
        assertEquals(1, documents().size());
        assertEquals(2990000.0, documents().getFirst().getMetadata().get("price"));
        assertEquals(0, documents().getFirst().getMetadata().get("stockOnHand"));
        when(repository.findAll()).thenReturn(List.of());
        assertEquals(0, service.reindexAll().documentCount()); assertTrue(documents().isEmpty());
    }

    @Test void partialProviderFailureKeepsEntirePreviousSnapshotAndReleasesLock() {
        var original = ProductChunkerTest.cpu(); when(repository.findAll()).thenReturn(List.of(original));
        service.reindexAll();
        var fresh = ProductChunkerTest.cpu(); fresh.setId(UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of(fresh, original));
        when(embedding.embed(any(Document.class))).thenReturn(new float[]{1, 0, 0})
                .thenThrow(new AiFailure(429, "VECTOR_QUOTA_EXCEEDED", "quota"));
        assertEquals(429, assertThrows(AiFailure.class, service::reindexAll).status);
        assertEquals(List.of(original.getId().toString()), documents().stream().map(d -> d.getMetadata().get("productId")).toList());
        when(repository.findAll()).thenReturn(List.of());
        assertEquals("SUCCESS", service.reindexAll().status());
    }

    @Test void databaseFailureKeepsOldStoreWithoutCallingProvider() {
        when(repository.findAll()).thenReturn(List.of(ProductChunkerTest.cpu())); service.reindexAll();
        clearInvocations(embedding);
        when(repository.findAll()).thenThrow(new IllegalStateException("DB down"));
        assertThrows(IllegalStateException.class, service::reindexAll);
        verifyNoInteractions(embedding);
        assertEquals(1, documents().size());
    }

    @Test void databaseTransactionEndsBeforeNetworkEmbeddingStarts() {
        when(repository.findAll()).thenReturn(List.of(ProductChunkerTest.cpu()));
        service.reindexAll();
        var order = inOrder(transactions, embedding);
        order.verify(transactions).commit(any());
        order.verify(embedding).embed(any(Document.class));
    }

    @Test void missingKeyDoesNotReadDatabaseOrCallProvider() {
        var noKey = new AiSettings("", "unused", settings.baseUrl(), 2000, 20, 100, 1000, false);
        var disabled = new ProductVectorIngestionService(repository, new ProductChunker(), store, noKey, transactions);
        assertEquals("VECTOR_NOT_CONFIGURED", assertThrows(AiFailure.class, disabled::reindexAll).code);
        verifyNoInteractions(repository, embedding);
    }

    @Test void concurrentAdminRequestGetsConflictWhileOldSnapshotRemainsReadable() throws Exception {
        when(repository.findAll()).thenReturn(List.of(ProductChunkerTest.cpu())); service.reindexAll();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(embedding.embed(any(Document.class))).thenAnswer(invocation -> {
            entered.countDown(); assertTrue(release.await(5, TimeUnit.SECONDS)); return new float[]{1, 0, 0};
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var running = executor.submit(service::reindexAll);
            try {
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                assertEquals(409, assertThrows(AiFailure.class, service::reindexAll).status);
                assertEquals(1, documents().size());
            } finally { release.countDown(); }
            assertEquals("SUCCESS", running.get(5, TimeUnit.SECONDS).status());
        }
    }
}
