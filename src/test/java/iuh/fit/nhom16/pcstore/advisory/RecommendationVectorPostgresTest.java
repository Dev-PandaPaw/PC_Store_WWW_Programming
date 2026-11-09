package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.infrastructure.InclusiveCosinePgVectorStore;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.sql.DriverManager;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real cosine SQL and Spring AI adapter; no provider key or network inference.
 * Owns only an isolated random schema in an explicitly configured test database with pgvector installed. */
@EnabledIfEnvironmentVariable(named="RECOMMENDATION_TEST_DB_URL",matches=".+")
class RecommendationVectorPostgresTest {
    SingleConnectionDataSource datasource;
    JdbcTemplate jdbc;
    String schema;
    InclusiveCosinePgVectorStore store;
    @BeforeEach void setup() throws Exception {
        var connection=DriverManager.getConnection(System.getenv("RECOMMENDATION_TEST_DB_URL"),
                System.getenv().getOrDefault("RECOMMENDATION_TEST_DB_USER","postgres"),
                System.getenv().getOrDefault("RECOMMENDATION_TEST_DB_PASSWORD","postgres"));
        datasource=new SingleConnectionDataSource(connection,true); jdbc=new JdbcTemplate(datasource);
        schema="recommendation_test_"+UUID.randomUUID().toString().replace("-","");
        jdbc.execute("CREATE SCHEMA "+schema);
        String namespace=jdbc.queryForObject("SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='vector'",String.class);
        assertNotNull(namespace,"Enable pgvector before running database tests");
        assertTrue(namespace.matches("[a-zA-Z_][a-zA-Z_0-9]*"));
        jdbc.execute("SET search_path TO "+schema+",public,\""+namespace+"\"");
        jdbc.execute("CREATE TABLE "+schema+".vectors (id UUID PRIMARY KEY,content TEXT,metadata JSON,embedding vector(3))");
        var embeddings=mock(EmbeddingModel.class); when(embeddings.embed(anyString())).thenReturn(new float[]{1,0,0});
        store=new InclusiveCosinePgVectorStore(PgVectorStore.builder(jdbc,embeddings).schemaName(schema)
                .vectorTableName("vectors").dimensions(3).indexType(PgVectorStore.PgIndexType.NONE)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE).initializeSchema(false));
    }
    @AfterEach void close() {
        if(jdbc!=null && schema!=null) jdbc.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
        if(datasource!=null) datasource.destroy();
    }
    UUID insert(String vector,String category) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO "+schema+".vectors VALUES (?, 'component', json_build_object('productId',?::text,'category',?::text),?::vector)",id,id,category,vector);
        return id;
    }
    @Test void actualPgvectorScoresMatchCosineFormulaAndThreshold() {
        UUID best=insert("[4,3,0]","cpu");
        insert("[3,4,0]","cpu"); insert("[0,1,0]","cpu"); insert("[1,0,0]","mainboard");
        var results=store.similaritySearch(SearchRequest.builder().query("cpu").topK(5).similarityThreshold(.70)
                .filterExpression("category == 'cpu'").build());
        assertEquals(1,results.size()); assertEquals(best.toString(),results.getFirst().getId());
        double expected=4.0/(Math.sqrt(4*4+3*3)*Math.sqrt(1));
        assertEquals(expected,results.getFirst().getScore(),1e-6);
    }
    @Test void exactScoreBoundaryIsIncludedAndTopKIsRespected() {
        UUID boundary=insert("[4,3,0]","cpu");
        Double nativeScore=jdbc.queryForObject("SELECT 1-(embedding <=> '[1,0,0]'::vector) FROM "+schema+".vectors WHERE id=?",Double.class,boundary);
        var exact=store.similaritySearch(SearchRequest.builder().query("cpu").topK(5).similarityThreshold(nativeScore).build());
        assertEquals(1,exact.size()); assertEquals(boundary.toString(),exact.getFirst().getId());
        for(int i=0;i<8;i++) insert("[1,0,0]","cpu");
        assertEquals(5,store.similaritySearch(SearchRequest.builder().query("cpu").topK(5).similarityThreshold(.70).build()).size());
    }
}
