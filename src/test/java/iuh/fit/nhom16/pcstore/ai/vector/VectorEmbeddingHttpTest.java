package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiSettings;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Spring AI + Google SDK + HTTP + SimpleVectorStore; only provider/repository are fixtures. */
@EnabledIfSystemProperty(named = "vector.simulation.url", matches = ".+")
class VectorEmbeddingHttpTest {
    AnnotationConfigApplicationContext context;
    ProductVectorStore store;
    MockMvc mvc;
    String base;

    @BeforeEach void setup() throws Exception {
        base = System.getProperty("vector.simulation.url"); control("normal");
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fixture", Map.of(
                "app.ai.api-key", "simulation-key-not-real", "app.ai.base-url", base,
                "app.ai.allow-local-mocks", "true", "app.ai.timeout-ms", "800")));
        context.register(VectorStoreConfig.class); context.refresh();
        store = context.getBean(ProductVectorStore.class);
        var repository = mock(ProductRepository.class);
        when(repository.findAll()).thenReturn(List.of(ProductChunkerTest.cpu()));
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var service = new ProductVectorIngestionService(repository, new ProductChunker(), store,
                context.getBean(AiSettings.class), manager);
        mvc = MockMvcBuilders.standaloneSetup(new AdminVectorController(service)).build();
    }

    @AfterEach void close() throws Exception { if (context != null) context.close(); control("normal"); }

    void control(String mode) throws Exception {
        var result = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/__control"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"mode\":\"" + mode + "\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, result.statusCode());
    }

    @Test void httpPipelineEmbeds768DimensionsAndStoresProductMetadata() throws Exception {
        mvc.perform(post("/api/admin/vector/reindex")).andExpect(status().isOk())
                .andExpect(jsonPath("$.productCount").value(1)).andExpect(jsonPath("$.documentCount").value(1));
        assertEquals(768, context.getBean(EmbeddingModel.class).embed("CPU Intel").length);
        var found = store.similaritySearch(SearchRequest.builder().query("CPU Intel").topK(5).similarityThreshold(0.7).build());
        assertEquals(1, found.size());
        assertEquals(ProductChunkerTest.cpu().getId().toString(), found.getFirst().getMetadata().get("productId"));
        assertEquals(3200000.0, found.getFirst().getMetadata().get("price"));
        assertEquals(1.0, found.getFirst().getScore(), 0.0001);
    }

    @ParameterizedTest(name = "{0} returns {1} and retains old vectors without retry")
    @CsvSource({"quota,429,VECTOR_QUOTA_EXCEEDED", "access,503,VECTOR_ACCESS_DENIED",
            "provider,502,VECTOR_EMBEDDING_FAILED", "timeout,502,VECTOR_EMBEDDING_FAILED",
            "wrongdimension,502,VECTOR_EMBEDDING_FAILED", "zero,502,VECTOR_EMBEDDING_FAILED", "empty,502,VECTOR_EMBEDDING_FAILED"})
    void failedProviderNeverPublishesBrokenIndex(String mode, int status, String code) throws Exception {
        mvc.perform(post("/api/admin/vector/reindex")).andExpect(status().isOk());
        control(mode);
        mvc.perform(post("/api/admin/vector/reindex")).andExpect(status().is(status))
                .andExpect(jsonPath("$.error.code").value(code));
        var stats = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/__stats")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertTrue(stats.body().contains("\"requests\":1"), "No retry at Spring AI or Google SDK level: " + stats.body());
        control("normal");
        var found = store.similaritySearch(SearchRequest.builder().query("CPU").build());
        assertEquals(1, found.size());
        assertEquals(ProductChunkerTest.cpu().getId().toString(), found.getFirst().getMetadata().get("productId"));
    }
}
