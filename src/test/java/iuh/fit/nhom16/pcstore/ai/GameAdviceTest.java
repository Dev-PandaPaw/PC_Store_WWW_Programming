package iuh.fit.nhom16.pcstore.ai;

import iuh.fit.nhom16.pcstore.advisory.*;
import iuh.fit.nhom16.pcstore.catalog.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameAdviceTest {
    @Test void extractsAnyNamedGameAndKeepsOnlyUserGameContext() {
        assertEquals("Black Myth: Wukong",GameRequirementsService.gameTitle(List.of("Máy tính có cấu hình chơi Black Myth: Wukong?")));
        assertEquals("Elden Ring",GameRequirementsService.gameTitle(List.of("PC chơi Elden Ring dưới 30 triệu")));
        assertEquals("Fixture Quest",GameRequirementsService.gameTitle(List.of("Tôi muốn máy chạy Fixture Quest?")));
        assertEquals("Black Myth: Wukong",GameRequirementsService.gameTitle(List.of("PC chơi Wukong","Nếu tăng ngân sách lên 30 triệu?")));
        assertNull(GameRequirementsService.gameTitle(List.of("PC chơi Wukong","Chuột giá rẻ chơi game")));
        assertNull(GameRequirementsService.gameTitle(List.of("PC chơi game dưới 25 triệu")));
        assertNull(GameRequirementsService.gameTitle(List.of("PC chơi game [test:provider]")));
    }
    @Test void cheapRulesApplyAfterBudgetAvailabilityAndCategoryAndKeepPriceTies() throws Exception {
        var catalog=mock(StorefrontCatalog.class); when(catalog.products()).thenReturn(ProductIntelligenceTest.products());
        var ai=ProductIntelligenceTest.unavailable();
        var search=new SmartSearchService(catalog,ai);
        for(String query:List.of("Chuột giá rẻ chơi game","Chuột giá rẻ nhất chơi game","chuot re nhat","Chuột rẻ nhất dưới 1 triệu")) {
            var result=search.search(query,"ALL","price-desc",false,"client");
            assertEquals(1,result.products().size(),query);
            assertTrue(result.products().getFirst().name().contains("G102"),query);
            assertTrue(result.criteria().cheapest()); assertTrue(result.summary().contains("thấp nhất"));
        }
        assertTrue(search.search("Chuột rẻ nhất còn hàng","ALL","featured",false,"client").products().isEmpty());
        assertFalse(search.search("Chuột giá rẻ dưới 1 triệu","ALL","featured",false,"client").criteria().cheapest());
        var lowest=search.search("Chuột rẻ nhất","ALL","featured",false,"client").products().getFirst();
        var tie=new StoreProduct("tie-id",null,"tie-product","Tie mouse","mouse","brand","",lowest.price(),lowest.price(),lowest.specs(),1,lowest.image());
        when(catalog.products()).thenReturn(List.of(lowest,tie));
        assertEquals(2,search.search("Chuột rẻ nhất","ALL","featured",false,"client").total());
        var advisor=new AiAdvisorService(catalog,ai,ProductIntelligenceTest.JSON);
        var answer=advisor.advise(new AiAdvisorService.ChatRequest("Chuột giá rẻ chơi game",List.of()),"client");
        assertEquals("recommendations",answer.outcome()); assertEquals(2,answer.products().size());
        assertTrue(answer.message().contains("thấp nhất")); assertFalse(answer.message().contains("FPS"));
    }
    @Test void failedRequirementLookupNeverInventsGameFactsOrProducts() {
        var ai=ProductIntelligenceTest.unavailable();
        when(ai.askGrounded(anyString(),any(),any(),anyString())).thenThrow(new AiFailure(503,"UNAVAILABLE","Unavailable"));
        var catalog=mock(StorefrontCatalog.class);
        var advisor=new AiAdvisorService(catalog,ai,ProductIntelligenceTest.JSON);
        var answer=advisor.advise(new AiAdvisorService.ChatRequest("Máy chơi Totally Unknown Quest?",List.of()),"client");
        assertEquals("clarification",answer.outcome()); assertTrue(answer.products().isEmpty()); assertNull(answer.game());
        verifyNoInteractions(catalog);
    }
    @Test void verifiedWukongReferenceHasSourceAndLookupDate() {
        var service=new GameRequirementsService(ProductIntelligenceTest.unavailable(),ProductIntelligenceTest.JSON);
        var game=service.lookup("Black Myth: Wukong","client").orElseThrow();
        assertEquals(16,game.minimum().ramGb()); assertEquals(130,game.recommended().storageGb());
        assertTrue(game.recommended().ssdRequired()); assertEquals("verified_reference",game.lookupStatus());
        assertTrue(game.sources().getFirst().url().contains("2358720"));
    }
}
