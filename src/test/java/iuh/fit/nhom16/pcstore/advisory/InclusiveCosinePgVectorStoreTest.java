package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.infrastructure.InclusiveCosinePgVectorStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InclusiveCosinePgVectorStoreTest {
    /** Exercise the actual Spring AI SQL generation without requiring a database. */
    static class CapturingJdbc extends JdbcTemplate {
        String sql;
        Object[] parameters;
        @Override @SuppressWarnings("unchecked")
        public <T> List<T> query(String sql,RowMapper<T> mapper,Object... parameters) {
            this.sql=sql; this.parameters=parameters;
            return (List<T>)List.of(Document.builder().text("boundary").score(.70).build(),
                    Document.builder().text("below boundary").score(Math.nextDown(.70)).build());
        }
    }
    @Test void vendorSqlIncludesExactThresholdButPostfilterRejectsLowerScores() {
        var jdbc=new CapturingJdbc();
        var embeddings=mock(EmbeddingModel.class);
        when(embeddings.embed("cpu")).thenReturn(new float[]{1,0,0});
        var store=new InclusiveCosinePgVectorStore(PgVectorStore.builder(jdbc,embeddings).dimensions(3)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE).initializeSchema(false));
        var request=SearchRequest.builder().query("cpu").topK(5).similarityThreshold(.70).build();
        var result=store.similaritySearch(request);
        assertTrue(jdbc.sql.contains("<=>"));
        assertTrue((Double)jdbc.parameters[2] > 1-.70,"Strict SQL distance comparison must admit the boundary");
        assertEquals(5,jdbc.parameters[3]);
        assertEquals(1,result.size()); assertEquals(.70,result.getFirst().getScore());
        assertEquals(.70,request.getSimilarityThreshold(),"The caller's request must remain unchanged");
    }
}
