package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: two real provider requests with a fixture product; no database writes or UI dependency. */
@EnabledIfSystemProperty(named = "vector.live", matches = "true")
class VectorEmbeddingLiveTest {
    @Test void realGeminiEmbeddingIs768DimensionsAndCanBeLoadedIntoSimpleVectorStore() throws Exception {
        var properties = new Properties();
        if (Files.exists(Path.of(".env"))) try (var reader = Files.newBufferedReader(Path.of(".env"))) { properties.load(reader); }
        String key = System.getenv().getOrDefault("GEMINI_API_KEY", properties.getProperty("GEMINI_API_KEY", ""));
        assertFalse(key.isBlank(), "Configure GEMINI_API_KEY before running the opt-in live test");
        var config = new VectorStoreConfig();
        var settings = new AiSettings(key, "unused", "https://generativelanguage.googleapis.com", 20000, 20, 100, 1000, false);
        String model = System.getenv().getOrDefault("GEMINI_EMBEDDING_MODEL", properties.getProperty("GEMINI_EMBEDDING_MODEL", "gemini-embedding-001"));
        try (var client = config.productEmbeddingClient(settings)) {
            var embedding = config.productEmbeddingModel(client, settings, model, 768);
            var document = new ProductChunker().chunk(ProductChunkerTest.cpu()).getFirst();
            var store = new ProductVectorStore(embedding);
            store.replaceAll(List.of(document)); // Request 1: embed document + load.
            assertEquals(768, embedding.dimensions());
            // Request 2: embed the same text as a query, then cosine similarity in the real store.
            var found = store.similaritySearch(org.springframework.ai.vectorstore.SearchRequest.builder().query(document.getText()).build());
            assertEquals(1, found.size()); assertEquals(document.getId(), found.getFirst().getId());
            assertEquals(1.0, found.getFirst().getScore(), 0.0001);
            System.out.println("LIVE GEMINI PASS: model=" + model + ", dimensions=" + embedding.dimensions() + ", storedDocuments=" + found.size());
        }
    }
}
