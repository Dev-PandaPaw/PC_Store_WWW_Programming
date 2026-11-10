package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.infrastructure.SemanticRecommendationConfiguration;
import iuh.fit.nhom16.pcstore.ai.vector.*;
import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SemanticRecommendationConfigurationTest {
    @Test void packagedFlywayLocationsHaveNoDuplicateVersions() throws Exception {
        var versions=new java.util.HashMap<Integer,java.nio.file.Path>();
        var pattern=java.util.regex.Pattern.compile("V(\\d+)__.*\\.sql");
        for(String location:java.util.List.of("migration","orders","recommendations")) {
            var resource=getClass().getClassLoader().getResource("db/"+location);
            assertNotNull(resource);
            try(var files=java.nio.file.Files.walk(java.nio.file.Path.of(resource.toURI()))) {
                for(var file:files.filter(java.nio.file.Files::isRegularFile).toList()) {
                    var matcher=pattern.matcher(file.getFileName().toString());
                    if(!matcher.matches()) continue;
                    var previous=versions.putIfAbsent(Integer.parseInt(matcher.group(1)),file);
                    assertNull(previous,()->"Duplicate migration: "+previous+" and "+file);
                }
            }
        }
        assertTrue(versions.keySet().containsAll(java.util.List.of(15,16,17)));
    }
    final ApplicationContextRunner runner=new ApplicationContextRunner()
            .withUserConfiguration(VectorStoreConfig.class,SemanticRecommendationConfiguration.class)
            .withBean(JdbcTemplate.class,()->mock(JdbcTemplate.class));
    @Test void normalProfileKeepsExactlyOneMemoryStoreAndEmbeddingModel() {
        runner.run(context->{
            assertNull(context.getStartupFailure());
            assertEquals(1,context.getBeansOfType(VectorStore.class).size());
            assertEquals(1,context.getBeansOfType(EmbeddingModel.class).size());
            assertFalse(context.getBean(ProductVectorStore.class).isPersistent());
        });
    }
    @Test void semanticProfileSelectsExactlyOnePersistentStoreAndSharesEmbedding() {
        runner.withPropertyValues("app.recommendations.enabled=true").run(context->{
            assertNull(context.getStartupFailure());
            assertEquals(1,context.getBeansOfType(VectorStore.class).size());
            assertEquals(1,context.getBeansOfType(EmbeddingModel.class).size());
            assertTrue(context.getBean(ProductVectorStore.class).isPersistent());
            assertEquals(768,context.getBean(EmbeddingModel.class).dimensions());
            assertEquals("VECTOR_NOT_CONFIGURED",assertThrows(AiFailure.class,()->context.getBean(EmbeddingModel.class).embed("CPU")).code);
        });
    }
    @Test void mismatchedDimensionsFailBeforeAnyProviderCall() {
        runner.withPropertyValues("app.recommendations.enabled=true","app.ai.vector.dimensions=256")
                .run(context->assertNotNull(context.getStartupFailure()));
    }
}
