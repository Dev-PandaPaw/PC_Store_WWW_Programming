package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.*;
import iuh.fit.nhom16.pcstore.ai.vector.*;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.application.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Spring AI embeddings + vector search + ChatClient over real HTTP; external Google endpoints are fixtures. */
@EnabledIfSystemProperty(named="ai.simulation.url",matches=".+")
@EnabledIfSystemProperty(named="vector.simulation.url",matches=".+")
class ComponentBuildHttpTest {
    AnnotationConfigApplicationContext context;
    AiGateway ai;
    AiAdvisorService advisor;
    MockMvc mvc;
    JsonMapper json=JsonMapper.builder().build();
    String chatBase,vectorBase;
    @BeforeEach void setup() throws Exception {
        chatBase=System.getProperty("ai.simulation.url");
        vectorBase=System.getProperty("vector.simulation.url");
        control(chatBase,"normal"); control(vectorBase,"normal");
        context=new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fixture",Map.of(
                "app.ai.api-key","simulation-key-not-real","app.ai.base-url",vectorBase,
                "app.ai.allow-local-mocks","true","app.ai.timeout-ms","1000")));
        context.register(VectorStoreConfig.class); context.refresh();
        var vectors=context.getBean(ProductVectorStore.class);
        var items=ComponentBuildAdvisorTest.fixtures();
        vectors.add(items.stream().map(item->new Document(item.product().searchable(),Map.of("productId",item.id().toString()))).toList());
        var catalog=mock(RecommendationCatalog.class);
        when(catalog.indexableProducts()).thenReturn(items);
        when(catalog.findAll(anySet())).thenReturn(items.stream().collect(Collectors.toMap(RecommendationCatalog.Item::id,i->i)));
        var settings=new AiSettings("simulation-key-not-real","gemini-3.5-flash-lite",chatBase,2500,100,1000,10000,true);
        ai=new AiGateway(settings,new AiLimiter(settings),json);
        var retrieval=new ProductRetrievalService(vectors,mock(ProductVectorIngestionService.class));
        var builds=new ComponentBuildAdvisor(catalog,retrieval,ai,json);
        var storefront=mock(StorefrontCatalog.class); when(storefront.products()).thenReturn(items.stream().map(RecommendationCatalog.Item::product).toList());
        advisor=new AiAdvisorService(storefront,ai,json,mock(GameAdvisor.class),builds,retrieval);
        mvc=MockMvcBuilders.standaloneSetup(new AiAdvisorController(advisor,ai,json)).setControllerAdvice(new AiExceptionHandler()).build();
    }
    @AfterEach void close() throws Exception {
        if(ai!=null) ai.destroy(); if(context!=null) context.close();
        control(chatBase,"normal"); control(vectorBase,"normal");
    }
    void control(String base,String mode) throws Exception {
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base+"/__control"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"mode\":\""+mode+"\"}")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());
    }
    String request(String message) { return json.writeValueAsString(new AiAdvisorService.ChatRequest(message,List.of())); }
    @Test void realHttpEmbeddingRetrievalAndStructuredChoiceProduceCompleteBuild() throws Exception {
        mvc.perform(post("/api/ai/chat").contentType("application/json").content(request("Build PC linh kiện rời chơi Valorant dưới 20 triệu")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("recommendations"))
                .andExpect(jsonPath("$.products.length()").value(8)).andExpect(jsonPath("$.build.total").value(12_500_000))
                .andExpect(jsonPath("$.build.retrieval").value("vector")).andExpect(jsonPath("$.build.status").value("needs_verification"));
        var followup=advisor.advise(new AiAdvisorService.ChatRequest("Ngân sách 20 triệu",List.of(
                new AiAdvisorService.HistoryItem("user","Mình muốn build linh kiện rời thành PC"),
                new AiAdvisorService.HistoryItem("assistant","Bạn có ngân sách bao nhiêu?"))),"client");
        assertEquals(8,followup.products().size()); assertEquals(20_000_000,followup.build().budget());
    }
    @Test void forgedBuildAndProviderErrorsNeverReturnUnverifiedCards() throws Exception {
        control(chatBase,"forgedbuild");
        mvc.perform(post("/api/ai/chat").contentType("application/json").content(request("Build PC dưới 20 triệu")))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.error.code").value("AI_INVALID_BUILD"));
        control(vectorBase,"quota");
        mvc.perform(post("/api/ai/chat").contentType("application/json").content(request("Build PC dưới 20 triệu")))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.error.code").value("VECTOR_QUOTA_EXCEEDED"));
    }
}
