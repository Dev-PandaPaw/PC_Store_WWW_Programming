package iuh.fit.nhom16.pcstore.ai;

import iuh.fit.nhom16.pcstore.advisory.*;
import iuh.fit.nhom16.pcstore.catalog.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual Spring AI + Google SDK HTTP requests; external provider is a Postman simulation. */
@EnabledIfSystemProperty(named="ai.simulation.url", matches=".+")
class SpringAiHttpTest {
    private AiGateway ai;
    private SmartSearchService search;
    private AiAdvisorService advisor;
    private StorefrontCatalog catalog;
    private MockMvc mvc;
    @BeforeEach void setup() throws Exception {
        control("normal");
        var settings=new AiSettings("simulation-key-not-real","gemini-3.5-flash-lite",System.getProperty("ai.simulation.url"),2500,100,1000,10000,true);
        ai=new AiGateway(settings,new AiLimiter(settings),ProductIntelligenceTest.JSON);
        catalog=mock(StorefrontCatalog.class); when(catalog.products()).thenReturn(ProductIntelligenceTest.products());
        search=new SmartSearchService(catalog,ai);
        advisor=new AiAdvisorService(catalog,ai,ProductIntelligenceTest.JSON);
        mvc=MockMvcBuilders.standaloneSetup(new SmartSearchController(search),new AiAdvisorController(advisor,ai,ProductIntelligenceTest.JSON))
                .setControllerAdvice(new AiExceptionHandler()).build();
    }
    @AfterEach void close() throws Exception { control("normal"); if(ai!=null) ai.destroy(); }
    private void control(String mode) throws Exception {
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(System.getProperty("ai.simulation.url")+"/__control"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"mode\":\""+mode+"\"}")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());
    }
    private AiAdvisorService.ChatResponse chat(String question) { return advisor.advise(new AiAdvisorService.ChatRequest(question,List.of()),"client"); }
    @Test void springAiInterpretsSearchAndCachesIntentWithoutCachingProducts() throws Exception {
        var result=search.search("chuột giá rẻ chơi game","ALL","featured",false,"client");
        assertEquals("ai",result.mode()); assertFalse(result.products().isEmpty());
        assertTrue(result.products().stream().allMatch(p -> p.category().equals("mouse")));
        assertTrue(result.criteria().cheaper());
        var original=result.products().getFirst();
        var updated=new StoreProduct(original.id(),original.catalogKey(),original.slug(),original.name(),original.category(),original.brand(),original.description(),original.price()+1000,original.originalPrice(),original.specs(),2,original.image());
        when(catalog.products()).thenReturn(ProductIntelligenceTest.products().stream().map(p -> p.id().equals(updated.id()) ? updated : p).toList());
        var synonymous=search.search("con trỏ không dây bắn súng","ALL","featured",false,"client");
        assertEquals("ai",synonymous.mode()); assertEquals(List.of("mouse"),synonymous.criteria().groups());
        assertFalse(synonymous.products().isEmpty());
        control("provider");
        var cached=search.search("chuột giá rẻ chơi game","ALL","featured",false,"client");
        assertEquals("ai",cached.mode());
        assertEquals(updated.price(),cached.products().stream().filter(p -> p.id().equals(updated.id())).findFirst().orElseThrow().price());
        assertEquals(2,cached.products().stream().filter(p -> p.id().equals(updated.id())).findFirst().orElseThrow().stockOnHand());
        mvc.perform(get("/api/catalog/search").param("keyword","chuột dưới 1 triệu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("local"))
                .andExpect(jsonPath("$.products[0].category").value("mouse"));
    }
    @Test void explicitConstraintsCannotBeRelaxedByModel() throws Exception {
        control("relax");
        var result=search.search("chuột không dây dưới 1 triệu","ALL","featured",false,"client");
        assertEquals("ai",result.mode()); assertEquals(List.of("mouse"),result.criteria().groups());
        assertEquals(1000000L,result.criteria().maxBudget());
        assertTrue(result.products().stream().allMatch(p -> p.category().equals("mouse") && p.price()<=1000000));
    }
    @Test void invalidAiIntentAndTimeoutKeepSearchUsable() throws Exception {
        control("badintent");
        var result=search.search("PC cá nhân","ALL","featured",false,"client");
        assertEquals("local",result.mode()); assertFalse(result.products().isEmpty());
        control("timeout");
        var timeout=search.search("chuột giá rẻ chơi game","ALL","featured",false,"client");
        assertEquals("local",timeout.mode()); assertFalse(timeout.products().isEmpty());
    }
    @Test void advisorPreservesContractAndFollowUp() {
        var first=chat("PC chơi game dưới 25 triệu");
        assertEquals("recommendations",first.outcome()); assertFalse(first.products().isEmpty());
        var next=advisor.advise(new AiAdvisorService.ChatRequest("Nếu tăng lên 30 triệu thì sao?",List.of(
                new AiAdvisorService.HistoryItem("user","PC chơi game dưới 25 triệu"),
                new AiAdvisorService.HistoryItem("assistant",first.message()))),"client");
        assertEquals(30000000L,next.criteria().maxBudget()); assertFalse(next.products().isEmpty());
        assertTrue(next.products().stream().allMatch(p -> p.get("category").equals("pc-gaming") && ((Number)p.get("price")).longValue() <= 30000000));
    }
    @Test void rejectsForgedProductsPricesAndMalformedModelOutput() {
        for (String scenario:List.of("forged","prices","invalid"))
            assertEquals(502,assertThrows(AiFailure.class,() -> chat("Chuột dưới 1 triệu [test:"+scenario+"]")).status);
        assertEquals("clarification",chat("PC chơi game [test:clarify]").outcome());
        assertEquals("out_of_scope",chat("PC chơi game [test:offtopic]").outcome());
    }
    @Test void mapsProviderErrorsAndQuotaWithoutLeakingUpstreamBodies() {
        assertEquals(502,assertThrows(AiFailure.class,() -> chat("Chuột dưới 1 triệu [test:provider]")).status);
        assertEquals(503,assertThrows(AiFailure.class,() -> chat("Chuột dưới 1 triệu [test:access]")).status);
        assertEquals(504,assertThrows(AiFailure.class,() -> chat("Chuột dưới 1 triệu [test:timeout]")).status);
        var quota=assertThrows(AiFailure.class,() -> chat("Chuột dưới 1 triệu [test:quota]"));
        assertEquals(429,quota.status,() -> String.valueOf(quota.getCause())); assertEquals(60,quota.retryAfterSeconds);
        assertEquals("AI_QUOTA_EXCEEDED",assertThrows(AiFailure.class,() -> chat("Chuột dưới 1 triệu")).code);
        assertEquals("local",search.search("chuột giá rẻ chơi game","ALL","featured",false,"client").mode());
    }
    @Test void apiValidationAndErrorEnvelopeRemainCompatible() throws Exception {
        mvc.perform(post("/api/ai/chat").contentType("application/json").content("{\"message\":\"a\",\"history\":[],\"products\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/catalog/search").param("keyword","a".repeat(301))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/ai/status")).andExpect(status().isOk()).andExpect(jsonPath("$.maxTurns").value(8));
    }
    @Test void wukongUsesRequirementsAndChoosesRecommendedRatherThanAnyGamingPc() {
        var answer=chat("Máy tính có cấu hình chơi Black Myth: Wukong?");
        assertEquals("recommendations",answer.outcome()); assertNotNull(answer.game());
        assertEquals("Black Myth: Wukong",answer.game().title()); assertEquals("recommended",answer.game().selectedTarget());
        assertFalse(answer.products().isEmpty());
        assertTrue(answer.products().stream().allMatch(product -> product.get("name").toString().contains("5060")));
        var cheap=chat("Máy chơi Black Myth: Wukong rẻ nhất?");
        assertEquals(1,cheap.products().size()); assertTrue(cheap.products().getFirst().get("name").toString().contains("3050"));
        assertEquals("minimum",cheap.game().selectedTarget());
        var recommendedHistory=advisor.advise(new AiAdvisorService.ChatRequest("Chọn máy rẻ nhất",List.of(
                new AiAdvisorService.HistoryItem("user","Máy chơi Wukong mức khuyến nghị"),
                new AiAdvisorService.HistoryItem("assistant",answer.message()))),"client");
        assertEquals("recommended",recommendedHistory.game().selectedTarget());
        assertTrue(recommendedHistory.products().stream().allMatch(product -> product.get("name").toString().contains("5060")));
        var followUp=advisor.advise(new AiAdvisorService.ChatRequest("Ngân sách dưới 10 triệu",List.of(
                new AiAdvisorService.HistoryItem("user","Máy chơi Wukong?"),
                new AiAdvisorService.HistoryItem("assistant",answer.message()))),"client");
        assertEquals("no_match",followUp.outcome()); assertTrue(followUp.products().isEmpty());
    }
    @Test void groundedLookupSupportsUnlistedTitlesAndRejectsMissingCitations() throws Exception {
        var result=chat("Máy chơi Fixture Quest?");
        assertEquals("recommendations",result.outcome()); assertEquals("Fixture Quest",result.game().title());
        assertEquals("live_search",result.game().lookupStatus()); assertFalse(result.game().sources().isEmpty());
        control("noground");
        var absent=chat("Máy chơi Another Quest?");
        assertEquals("clarification",absent.outcome()); assertTrue(absent.products().isEmpty());
        control("badsource");
        assertEquals("clarification",chat("Máy chơi Third Quest?").outcome());
    }
    @Test void numericHardwareChecksOverrideModelApprovalAndRejectChangedSpecs() throws Exception {
        var base=ProductIntelligenceTest.products().stream().filter(p -> p.name().contains("5060")).findFirst().orElseThrow();
        var lowRam=new StoreProduct(base.id(),null,base.slug(),base.name(),base.category(),base.brand(),base.description(),base.price(),base.originalPrice(),
                List.of(new StoreProduct.Spec("Core i5-12400F"),new StoreProduct.Spec("RTX 5060 8GB"),new StoreProduct.Spec("RAM 8GB"),new StoreProduct.Spec("SSD 512GB")),1,base.image());
        when(catalog.products()).thenReturn(List.of(lowRam));
        control("allpass");
        assertEquals("no_match",chat("Máy tính chơi Wukong?").outcome());
        var noSpace=new StoreProduct(base.id(),null,base.slug(),base.name(),base.category(),base.brand(),base.description(),base.price(),base.originalPrice(),
                List.of(new StoreProduct.Spec("Core i5-12400F"),new StoreProduct.Spec("RTX 5060 8GB"),new StoreProduct.Spec("RAM 16GB"),new StoreProduct.Spec("SSD 64GB")),1,base.image());
        when(catalog.products()).thenReturn(List.of(noSpace));
        assertEquals("no_match",chat("Máy tính chơi Wukong?").outcome());
        when(catalog.products()).thenReturn(List.of(base),List.of(lowRam));
        assertEquals("no_match",chat("Máy tính chơi Wukong?").outcome());
        when(catalog.products()).thenReturn(List.of(base)); control("forgedcomparison");
        var freshAdvisor=new AiAdvisorService(catalog,ai,ProductIntelligenceTest.JSON);
        assertEquals("AI_INVALID_COMPARISON",assertThrows(AiFailure.class,() -> freshAdvisor.advise(new AiAdvisorService.ChatRequest("Máy chơi Wukong?",List.of()),"client")).code);
    }
}
