package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.*;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import iuh.fit.nhom16.pcstore.catalog.application.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises real Spring AI multimodal serialization and Google SDK HTTP using the existing Postman fixture. */
@EnabledIfSystemProperty(named="ai.simulation.url",matches=".+")
class HardwareAttachmentHttpTest {
    AiGateway ai;
    MockMvc mvc;
    JsonMapper json=JsonMapper.builder().build();
    @BeforeEach void setup() throws Exception {
        control("normal");
        var settings=new AiSettings("simulation-key-not-real","gemini-3.5-flash-lite",System.getProperty("ai.simulation.url"),2500,100,1000,10000,true);
        ai=new AiGateway(settings,new AiLimiter(settings),json);
        var storefront=mock(StorefrontCatalog.class);var catalog=mock(RecommendationCatalog.class);
        var ram=ComponentBuildAdvisorTest.item("ram",1_000_000,null,Map.of("memory_type","DDR4","capacity_gb",32));
        when(storefront.products()).thenReturn(List.of(ram.product()));when(catalog.indexableProducts()).thenReturn(List.of(ram));
        var retrieval=mock(ProductRetrievalService.class);when(retrieval.rank(anyString(),anyList())).thenAnswer(call->call.getArgument(1));
        var hardware=new HardwareMatchAdvisor(storefront,catalog,retrieval,mock(ComponentBuildAdvisor.class),ai,json);
        var advisor=new AiAdvisorService(storefront,ai,json,mock(GameAdvisor.class),null,retrieval,hardware);
        mvc=MockMvcBuilders.standaloneSetup(new HardwareAttachmentController(new HardwareAttachmentService(ai)),new AiAdvisorController(advisor,ai,json))
                .setControllerAdvice(new AiExceptionHandler()).build();
    }
    @AfterEach void close() throws Exception { if(ai!=null)ai.destroy();control("normal"); }
    void control(String mode) throws Exception {
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(System.getProperty("ai.simulation.url")+"/__control"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"mode\":\""+mode+"\"}")).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());
    }
    @Test void actualImageBytesAndPdfUseInlineMediaAndReturnReviewableFacts() throws Exception {
        byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=");
        for(var file:List.of(new MockMultipartFile("file","specs.png","image/png",png),new MockMultipartFile("file","specs.pdf","application/pdf","%PDF-1.7\nfixture".getBytes(StandardCharsets.US_ASCII)))) {
            mvc.perform(multipart("/api/ai/attachments/analyze").file(file)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.readable").value(true)).andExpect(jsonPath("$.components[0].model").value("Intel Core i5-12400F"));
            var stats=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(System.getProperty("ai.simulation.url")+"/__stats")).GET().build(),HttpResponse.BodyHandlers.ofString());
            assertTrue(stats.body().contains(file.getContentType()),stats.body());
            assertTrue(stats.body().contains("\"bytes\":"+file.getBytes().length),stats.body());
        }
    }
    @Test void extractedCurrentMachineCanFindDifferentRamModelAndRetainsCompatibilityChecks() throws Exception {
        var response=mvc.perform(multipart("/api/ai/attachments/analyze").file(new MockMultipartFile("file","specs.txt","text/plain","CPU: i5-12400F\nRAM: 16GB DDR4".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var extracted=json.readValue(response,HardwareAttachmentService.Extraction.class);
        var request=new AiAdvisorService.ChatRequest("Tìm RAM dưới 2 triệu",List.of(),new HardwareContext("current",extracted.components(),"specs.txt"));
        mvc.perform(post("/api/ai/chat").contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("recommendations"))
                .andExpect(jsonPath("$.products[0].hardwareFit.status").value("needs_verification"));
    }
    @Test void photoWithoutTypedQuestionProposesPartsAndShortReplyKeepsItsContext() throws Exception {
        var response=mvc.perform(multipart("/api/ai/attachments/analyze").file(new MockMultipartFile("file","specs.txt","text/plain","CPU: i5-12400F\nRAM: 16GB DDR4".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var extracted=json.readValue(response,HardwareAttachmentService.Extraction.class);
        var context=new HardwareContext("current",extracted.components(),"specs.txt");
        String question="Đề xuất linh kiện phù hợp với thông số trong file.";
        var initial=mvc.perform(post("/api/ai/chat").contentType("application/json")
                .content(json.writeValueAsString(new AiAdvisorService.ChatRequest(question,List.of(),context))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("recommendations"))
                .andExpect(jsonPath("$.products[0].category").value("ram"))
                .andReturn().getResponse().getContentAsString();
        String answer=json.readTree(initial).get("message").asString();
        var followUp=new AiAdvisorService.ChatRequest("hiệu năng",List.of(new AiAdvisorService.HistoryItem("user",question),
                new AiAdvisorService.HistoryItem("assistant",answer)),context);
        mvc.perform(post("/api/ai/chat").contentType("application/json").content(json.writeValueAsString(followUp)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.outcome").value("recommendations"))
                .andExpect(jsonPath("$.products[0].category").value("ram"));
    }
    @Test void invalidExtractionQuotaUnreadableAndUnsupportedFilesHaveExplicitOutcomes() throws Exception {
        control("badextraction");
        mvc.perform(multipart("/api/ai/attachments/analyze").file(new MockMultipartFile("file","x.txt","text/plain","specs".getBytes())))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.error.code").value("ATTACHMENT_INVALID_EXTRACTION"));
        control("unreadable");
        mvc.perform(multipart("/api/ai/attachments/analyze").file(new MockMultipartFile("file","x.txt","text/plain","specs".getBytes())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.readable").value(false));
        control("quota");
        mvc.perform(multipart("/api/ai/attachments/analyze").file(new MockMultipartFile("file","x.txt","text/plain","specs".getBytes())))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","60"));
        mvc.perform(multipart("/api/ai/attachments/analyze").file(new MockMultipartFile("file","x.svg","image/svg+xml","<svg/>".getBytes())))
                .andExpect(status().isUnsupportedMediaType());
    }
}
