package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.application.ProductSemanticRecommendationService;
import iuh.fit.nhom16.pcstore.advisory.infrastructure.*;
import iuh.fit.nhom16.pcstore.ai.AiSettings;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.JdbcRecommendationCatalog;
import iuh.fit.nhom16.pcstore.advisory.api.RecentCategoryProvider;
import iuh.fit.nhom16.pcstore.ai.vector.VectorStoreConfig;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in real Supabase/Postgres verification. Reads .env internally, never prints credentials.
 * All writes belong to an isolated random schema, removed after every test. */
@EnabledIfSystemProperty(named="recommendation.supabase",matches="true")
class SupabaseRecommendationIntegrationTest {
    SingleConnectionDataSource datasource;
    JdbcTemplate jdbc;
    String schema;
    String extensionSchema;
    final UUID user=UUID.randomUUID(),otherUser=UUID.randomUUID();
    UUID source,compatible,board;
    JdbcRecommendationCatalog catalog;
    RecentCategoryProvider history;
    InclusiveCosinePgVectorStore vectors;
    TransactionTemplate transactions;
    Properties local;
    static float[] vector(float first,float second) { float[] value=new float[768];value[0]=first;value[1]=second;return value; }
    String setting(String name,String fallback) { return System.getenv().getOrDefault(name,local.getProperty(name,fallback)).trim(); }
    @BeforeEach void setup() throws Exception {
        local=new Properties();
        if(Files.exists(Path.of(".env"))) try(var reader=Files.newBufferedReader(Path.of(".env"))) {local.load(reader);}
        String url=setting("RECOMMENDATION_TEST_DB_URL",setting("DB_URL",""));
        assertTrue(url.startsWith("jdbc:postgresql:"),"Configure DB_URL or RECOMMENDATION_TEST_DB_URL");
        String separator=url.contains("?")?"&":"?";
        try {
            var connection=DriverManager.getConnection(url+separator+"connectTimeout=10&socketTimeout=30",
                    setting("RECOMMENDATION_TEST_DB_USER",setting("DB_USERNAME","postgres")),
                    setting("RECOMMENDATION_TEST_DB_PASSWORD",setting("DB_PASSWORD","")));
            datasource=new SingleConnectionDataSource(connection,true);
        } catch(Exception e) { fail("Database connection failed ("+e.getClass().getSimpleName()+"); credentials are not printed."); }
        jdbc=new JdbcTemplate(datasource);
        extensionSchema=jdbc.queryForObject("SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace WHERE e.extname='vector'",String.class);
        assertNotNull(extensionSchema,"Enable vector on Supabase first");
        assertTrue(extensionSchema.matches("[a-zA-Z_][a-zA-Z_0-9]*"));
        schema="recommendation_check_"+UUID.randomUUID().toString().replace("-","");
        jdbc.execute("CREATE SCHEMA "+schema);
        jdbc.execute("SET search_path TO "+schema+",public,\""+extensionSchema+"\"");
        jdbc.execute("""
                CREATE TABLE users(id UUID PRIMARY KEY);
                CREATE TABLE categories(id UUID PRIMARY KEY,slug VARCHAR(150));
                CREATE TABLE products(id UUID PRIMARY KEY,category_id UUID REFERENCES categories(id),slug TEXT,name TEXT,
                    brand TEXT,description TEXT,price NUMERIC(15,2),status TEXT,stock_on_hand INT,reserved_quantity INT);
                CREATE TABLE storefront_products(product_id UUID PRIMARY KEY REFERENCES products(id),catalog_key TEXT,original_price NUMERIC(15,2),specs JSONB);
                CREATE TABLE product_images(id UUID PRIMARY KEY,product_id UUID,storage_key TEXT,is_cover BOOLEAN,sort_order INT);
                """);
        for(String migration:List.of("db/migration/V15__create_user_activity_logs.sql",
                "db/migration/V16__component_socket_specs.sql","db/recommendations/V17__semantic_vector_store.sql")) {
            try(var input=getClass().getClassLoader().getResourceAsStream(migration)) {
                assertNotNull(input); jdbc.execute(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        jdbc.update("INSERT INTO users VALUES (?),(?)",user,otherUser);
        source=product("cpu",100,1,0,"LGA1700"); compatible=product("cpu",110,5,1,"LGA1700");
        product("cpu",100,5,0,"AM5");product("cpu",100,0,0,"LGA1700");
        board=product("mainboard",100,5,0,"LGA1700");
        catalog=new JdbcRecommendationCatalog(jdbc,JsonMapper.builder().build());
        history=(userId,since,limit)->jdbc.query("SELECT c.slug FROM user_activity_logs l JOIN products p ON p.id=l.product_id JOIN categories c ON c.id=p.category_id WHERE l.user_id=? AND l.event_type='VIEW_PRODUCT' AND l.created_at>=? AND p.status='ACTIVE' GROUP BY c.slug ORDER BY count(*) DESC,max(l.created_at) DESC,c.slug LIMIT ?",(rs,row)->rs.getString(1),userId,java.sql.Timestamp.from(since),limit);
        var embedding=mock(EmbeddingModel.class);
        when(embedding.embed(anyString())).thenReturn(vector(1,0));
        when(embedding.embed(anyList(),any(EmbeddingOptions.class),any(BatchingStrategy.class)))
                .thenAnswer(call -> call.<List<Document>>getArgument(0).stream().map(d -> vector(.8f,.6f)).toList());
        vectors=new InclusiveCosinePgVectorStore(PgVectorStore.builder(jdbc,embedding).schemaName(schema)
                .vectorTableName("product_semantic_vectors").dimensions(768).initializeSchema(false)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE));
        transactions=new TransactionTemplate(new DataSourceTransactionManager(datasource));
    }
    UUID product(String category,int price,int stock,int reserved,String socket) {
        UUID categoryId=UUID.randomUUID(),id=UUID.randomUUID();
        jdbc.update("INSERT INTO categories VALUES (?,?)",categoryId,category);
        jdbc.update("INSERT INTO products VALUES (?,?,?,'Component','Brand','Explicit socket LGA1700',?,'ACTIVE',?,?)",id,categoryId,"fixture-"+id,price,stock,reserved);
        jdbc.update("INSERT INTO storefront_products VALUES (?,?,?,'[]'::jsonb)",id,"fixture-"+id,price);
        jdbc.update("INSERT INTO product_component_specs VALUES (?,?)",id,socket);
        return id;
    }
    void record(UUID userId,UUID productId) {
        jdbc.update("INSERT INTO user_activity_logs (user_id,session_id,event_type,product_id,category_slug) VALUES (?,'fixture-session','VIEW_PRODUCT',?,'forged-category')",userId,productId);
    }
    @AfterEach void cleanup() {
        try {
            if(jdbc!=null && schema!=null && schema.matches("recommendation_check_[a-f0-9]{32}"))
                jdbc.execute("DROP SCHEMA IF EXISTS "+schema+" CASCADE");
        } finally {if(datasource!=null) datasource.destroy();}
    }
    @Test void realMigrationsHistoryScopeAndIdempotentIndexSupportBothRecommendationFlows() {
        record(user,source);record(otherUser,board);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM user_activity_logs",Integer.class));
        assertEquals(List.of("cpu"),history.topCategories(user,Instant.now().minus(Duration.ofDays(30)),3));
        assertEquals(List.of("mainboard"),history.topCategories(otherUser,Instant.now().minus(Duration.ofDays(30)),3));
        var index=new ProductSemanticIndexService(catalog,vectors,jdbc,"fixture");
        assertEquals(5,transactions.execute(status -> index.synchronize()).updated());
        assertEquals(0,transactions.execute(status -> index.synchronize()).updated());
        var recommendations=new ProductSemanticRecommendationService(catalog,()->vectors,()->history,
                Clock.systemUTC(),"fixture:768:semantic-v1");
        var similar=recommendations.similar(source,List.of());
        assertEquals(1,similar.products().size());
        assertEquals(compatible.toString(),similar.products().getFirst().product().id());
        assertEquals(.8,similar.products().getFirst().similarityScore(),1e-6);
        var personal=recommendations.personalized(user,List.of(board));
        assertEquals(2,personal.products().size());
        assertTrue(personal.products().stream().allMatch(p -> p.compatibilityStatus().equals("socket_match") && p.product().stockOnHand()>0));
        jdbc.update("UPDATE products SET reserved_quantity=stock_on_hand WHERE id=?",compatible);
        assertTrue(recommendations.similar(source,List.of()).products().isEmpty());
        System.out.println("PASS Supabase migrations, cosine 768D, history isolation, live stock/socket guardrails (vector namespace: "+extensionSchema+")");
    }
    @Test @EnabledIfSystemProperty(named="recommendation.live",matches="true")
    void configuredGoogleEmbeddingProduces768FiniteDimensions() {
        String key=setting("GEMINI_API_KEY",""); assertFalse(key.isBlank(),"Configure GEMINI_API_KEY");
        new ApplicationContextRunner().withUserConfiguration(VectorStoreConfig.class,SemanticRecommendationConfiguration.class)
                .withPropertyValues("app.recommendations.enabled=true","app.ai.vector.model="+setting("GEMINI_EMBEDDING_MODEL","gemini-embedding-001"))
                .withBean(JdbcTemplate.class,()->jdbc)
                .withBean(AiSettings.class,()->new AiSettings(key,"gemini-3.5-flash-lite","https://generativelanguage.googleapis.com",20000,20,100,1000,false))
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    float[] embedded;
                    try { embedded=context.getBean(EmbeddingModel.class).embed("CPU Intel socket LGA1700 cho máy tính"); }
                    catch(Exception e) { fail("Live embedding failed ("+e.getClass().getSimpleName()+"); provider details are not printed."); return; }
                    assertEquals(768,embedded.length);
                    double norm=0;for(float value:embedded) {assertTrue(Float.isFinite(value));norm+=value*value;}
                    assertTrue(norm>0); System.out.println("PASS live Google embedding: 768 finite dimensions");
                });
    }
}
