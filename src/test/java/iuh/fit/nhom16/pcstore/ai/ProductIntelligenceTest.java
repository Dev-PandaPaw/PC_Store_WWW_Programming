package iuh.fit.nhom16.pcstore.ai;

import iuh.fit.nhom16.pcstore.advisory.AiAdvisorService;
import iuh.fit.nhom16.pcstore.catalog.*;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductIntelligenceTest {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static List<StoreProduct> products() throws Exception {
        return JSON.convertValue(JSON.readTree(Files.readString(Path.of("postman/mocks/pixelcore-catalog/catalog.json"))).get("products"),new TypeReference<List<StoreProduct>>() {});
    }
    static AiGateway unavailable() {
        AiGateway gateway = mock(AiGateway.class);
        when(gateway.ask(anyString(),any(),any(),anyString())).thenThrow(new AiFailure(503,"AI_NOT_CONFIGURED","Unavailable"));
        return gateway;
    }
    @Test void vietnameseBudgetsAndHardwareNumbers() {
        assertEquals(new CriteriaParser.Budget(null,1000000L),CriteriaParser.budget("Chuột dưới 1 triệu"));
        assertEquals(new CriteriaParser.Budget(null,1500000L),CriteriaParser.budget("Mình có 1tr5 mua bàn phím"));
        assertEquals(new CriteriaParser.Budget(null,1500000L),CriteriaParser.budget("Khoảng 1,5 triệu"));
        assertEquals(new CriteriaParser.Budget(20000000L,25000000L),CriteriaParser.budget("Từ 20 đến 25 triệu"));
        assertEquals(new CriteriaParser.Budget(null,25000000L),CriteriaParser.budget("Tối đa 25.000.000đ"));
        assertNull(CriteriaParser.budget("Màn hình 4K"));
        assertNull(CriteriaParser.budget("Intel i5-12400F RTX 5060"));
    }
    @Test void followUpReplacesBudgetAndKeepsRealInventory() throws Exception {
        var criteria = CriteriaParser.parse("Nếu tăng lên 30 triệu thì sao?",CriteriaParser.parse("PC chơi game dưới 25 triệu"));
        assertEquals(List.of("pc"),criteria.groups()); assertEquals(30000000L,criteria.maxBudget());
        assertFalse(ProductMatcher.candidates(products(),criteria).isEmpty());
        assertTrue(ProductMatcher.candidates(products(),CriteriaParser.parse("Chuột còn hàng dưới 1 triệu")).isEmpty());
        assertTrue(ProductMatcher.candidates(products(),CriteriaParser.parse("PC gaming RTX 4090 dưới 25 triệu")).isEmpty());
    }
    @Test void naturalLanguageSearchWorksWithoutProviderAndHonorsFilters() throws Exception {
        var catalog = mock(StorefrontCatalog.class); when(catalog.products()).thenReturn(products());
        var search = new SmartSearchService(catalog,unavailable());
        var mouse = search.search("chuột giá rẻ chơi game","ALL","featured",false,"client");
        assertEquals("local",mouse.mode()); assertFalse(mouse.products().isEmpty());
        assertTrue(mouse.products().stream().allMatch(p -> p.category().equals("mouse")));
        assertEquals(mouse.products().stream().mapToLong(StoreProduct::price).min().orElseThrow(),mouse.products().getFirst().price());
        assertEquals(List.of("mouse"),CriteriaParser.parse("Chuột máy tính giá rẻ").groups());
        assertEquals(List.of("psu"),CriteriaParser.parse("Nguồn cho PC cá nhân").groups());
        assertEquals(List.of("pc"),CriteriaParser.parse("PC gaming RTX 5060 dưới 25 triệu").groups());
        assertTrue(search.search("PC cá nhân","ALL","featured",false,"client").products().stream().allMatch(p -> p.category().startsWith("pc-")));
        assertFalse(search.search("PC cá nhân","ALL","featured",false,"client").products().isEmpty());
        var wireless=search.search("chuot khong day duoi 1 trieu","ALL","featured",false,"client");
        assertFalse(wireless.products().isEmpty()); assertTrue(wireless.products().stream().allMatch(p -> p.price() <= 1000000));
        assertTrue(search.search("chuột giá rẻ","cpu","featured",false,"client").products().isEmpty());
        assertTrue(search.search("chuột razer","ALL","featured",false,"client").products().isEmpty());
        assertTrue(search.search("laptop chơi game","ALL","featured",false,"client").products().isEmpty());
        assertFalse(search.search("G102","ALL","featured",false,"client").products().isEmpty());
        assertThrows(AiFailure.class,() -> search.search("x".repeat(301),"ALL","featured",false,"client"));
    }
    @Test void adviceUsesCurrentDatabaseValuesAndRejectsForgedOrOverBudgetSelections() throws Exception {
        var criteria=CriteriaParser.parse("Chuột dưới 1 triệu");
        var candidates=ProductMatcher.candidates(products(),criteria);
        var original=candidates.getFirst();
        var current=new StoreProduct(original.id(),original.catalogKey(),original.slug(),original.name(),original.category(),original.brand(),original.description(),original.price()+1000,original.originalPrice(),original.specs(),2,original.image());
        var service=new AiAdvisorService(mock(StorefrontCatalog.class),unavailable(),JSON);
        var decision=new AiAdvisorService.Decision("recommend",List.of(new AiAdvisorService.Recommendation(original.id(),List.of("budget","specs"))),"none");
        var card=service.verify(decision,candidates,List.of(current),criteria).getFirst();
        assertEquals(current.price(),((Number)card.get("price")).longValue()); assertEquals(2,card.get("stockOnHand"));
        assertEquals("/products/"+original.slug(),card.get("url"));
        assertThrows(AiFailure.class,() -> service.verify(decision,List.of(),List.of(current),criteria));
        var expensive=new StoreProduct(original.id(),null,original.slug(),original.name(),original.category(),original.brand(),original.description(),2000000,2000000,original.specs(),2,original.image());
        assertThrows(AiFailure.class,() -> service.verify(decision,candidates,List.of(expensive),criteria));
    }
    @Test void chatRejectsInvalidHistoryAndIncompleteMultiGroupAdvice() throws Exception {
        assertThrows(AiFailure.class,() -> AiAdvisorService.validate(new AiAdvisorService.ChatRequest("a",List.of(new AiAdvisorService.HistoryItem("assistant","a")))));
        assertThrows(AiFailure.class,() -> AiAdvisorService.validate(new AiAdvisorService.ChatRequest("x".repeat(1201),List.of())));
        var criteria=CriteriaParser.parse("PC và chuột dưới 25 triệu để chơi game");
        var candidates=ProductMatcher.candidates(products(),criteria);
        assertFalse(candidates.isEmpty());
        var service=new AiAdvisorService(mock(StorefrontCatalog.class),unavailable(),JSON);
        var one=new AiAdvisorService.Decision("recommend",List.of(new AiAdvisorService.Recommendation(candidates.getFirst().id(),List.of("specs"))),"none");
        assertThrows(AiFailure.class,() -> service.verify(one,candidates,products(),criteria));
    }
    @Test void limiterEnforcesLocalLimitAndGlobalCooldown() {
        var settings=new AiSettings("","model","https://generativelanguage.googleapis.com",20000,1,2,3,false);
        var limiter=new AiLimiter(settings);
        assertDoesNotThrow(() -> limiter.acquire("one").close());
        assertEquals(429,assertThrows(AiFailure.class,() -> limiter.acquire("one")).status);
        limiter.cooldown(60);
        assertEquals("AI_QUOTA_EXCEEDED",assertThrows(AiFailure.class,() -> limiter.acquire("two")).code);
    }
}
