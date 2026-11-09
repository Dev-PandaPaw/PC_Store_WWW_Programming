package iuh.fit.nhom16.pcstore.advisory.infrastructure;

import iuh.fit.nhom16.pcstore.ai.vector.ProductVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reuse development's validated embedding model; select one backing store per profile. */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="app.recommendations.enabled",havingValue="true")
public class SemanticRecommendationConfiguration {
    @Bean
    ProductVectorStore recommendationVectorStore(JdbcTemplate jdbc, EmbeddingModel productEmbeddingModel,
            @Value("${app.ai.vector.dimensions:768}") int dimensions) {
        if(dimensions!=768) throw new IllegalArgumentException("PgVector migration requires 768 embedding dimensions");
        var persistent=new InclusiveCosinePgVectorStore(PgVectorStore.builder(jdbc,productEmbeddingModel).dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW).vectorTableName("product_semantic_vectors")
                .initializeSchema(false));
        persistent.afterPropertiesSet();
        return new ProductVectorStore(persistent);
    }
}
