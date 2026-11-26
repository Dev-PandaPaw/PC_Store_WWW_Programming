package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

class VectorStoreConfigTest {
    final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(VectorStoreConfig.class);

    @Test void createsInjectableSpringAiBeansWithoutKeyAndFailsOnlyWhenEmbeddingIsRequested() {
        context.run(app -> {
            assertNull(app.getStartupFailure());
            assertInstanceOf(ProductVectorStore.class, app.getBean(VectorStore.class));
            assertEquals(768, app.getBean(EmbeddingModel.class).dimensions());
            assertEquals("VECTOR_NOT_CONFIGURED", assertThrows(AiFailure.class,
                    () -> app.getBean(EmbeddingModel.class).embed("CPU")).code);
        });
    }

    @Test void rejectsUntrustedProviderUrl() {
        context.withPropertyValues("app.ai.base-url=https://example.com").run(app -> assertNotNull(app.getStartupFailure()));
    }
}
