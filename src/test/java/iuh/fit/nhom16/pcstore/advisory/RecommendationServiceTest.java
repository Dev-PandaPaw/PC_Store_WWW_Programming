package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.api.RecentCategoryProvider;
import iuh.fit.nhom16.pcstore.advisory.application.*;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {
    static final UUID USER=UUID.randomUUID();
    static final Instant NOW=Instant.parse("2026-11-09T12:00:00Z");
    final RecommendationCatalog catalog=mock(RecommendationCatalog.class);
    final VectorStore vectors=mock(VectorStore.class);
    final RecentCategoryProvider behavior=mock(RecentCategoryProvider.class);
    final Map<UUID,Item> items=new LinkedHashMap<>();
    ProductSemanticRecommendationService service;
    @BeforeEach void setup() {
        service=new ProductSemanticRecommendationService(catalog,()->vectors,()->behavior,Clock.fixed(NOW,ZoneOffset.UTC),"test:768:semantic-v1");
        when(catalog.findAll(anySet())).thenAnswer(call -> {
            Map<UUID,Item> result=new LinkedHashMap<>();
            for (UUID id:call.<Set<UUID>>getArgument(0)) if(items.containsKey(id)) result.put(id,items.get(id));
            return result;
        });
        when(behavior.topCategories(eq(USER),any(),eq(3))).thenReturn(List.of("cpu"));
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
    }
    static Item item(String category,long price,int stock,int reserved,String status,String socket) {
        UUID id=UUID.randomUUID();
        return new Item(id,new StoreProduct(id.toString(),"key","slug","Component "+id,category,"Brand","Specs",
                price,price,List.of(new StoreProduct.Spec("hardware")),Math.max(0,stock-reserved),"image"),status,stock,reserved,socket);
    }
    Item add(String category,long price,int stock,int reserved,String status,String socket) {
        Item item=item(category,price,stock,reserved,status,socket); items.put(item.id(),item); return item;
    }
    static Document hit(Item item,Double score) { return hit(item.id().toString(),score); }
    static Document hit(String id,Double score) {
        return Document.builder().text("untrusted index content").metadata("productId",id).score(score).build();
    }
    @Test void thresholdIsInclusiveAndInvalidScoresAreRejected() {
        var exact=add("cpu",100,2,0,"ACTIVE","LGA1700");
        var high=add("cpu",100,2,0,"ACTIVE","LGA1700");
        var low=add("cpu",100,2,0,"ACTIVE","LGA1700");
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit(low,.6999),hit(exact,.70),hit(high,.91),
                hit(UUID.randomUUID().toString(),Double.NaN),hit("bad-id",.99),hit(exact,null),hit(low,1.1)));
        var result=service.personalized(USER,List.of());
        assertEquals(List.of(high.id().toString(),exact.id().toString()),result.products().stream().map(r->r.product().id()).toList());
        assertEquals("needs_verification",result.products().getFirst().compatibilityStatus());
        var request=ArgumentCaptor.forClass(SearchRequest.class); verify(vectors).similaritySearch(request.capture());
        assertEquals(5,request.getValue().getTopK()); assertEquals(.70,request.getValue().getSimilarityThreshold());
        assertTrue(request.getValue().getQuery().contains("cpu")); assertNotNull(request.getValue().getFilterExpression());
        verify(behavior).topCategories(USER,NOW.minus(Duration.ofDays(30)),3);
    }
    @Test void liveCatalogOverridesInventoryAndStatusFromIndex() {
        var good=add("cpu",150,3,1,"ACTIVE","LGA1700");
        var zero=add("cpu",100,0,0,"ACTIVE","LGA1700");
        var reserved=add("cpu",100,2,2,"ACTIVE","LGA1700");
        var inactive=add("cpu",100,5,0,"INACTIVE","LGA1700");
        var negative=add("cpu",100,-1,0,"ACTIVE","LGA1700");
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit(zero,.99),hit(reserved,.98),
                hit(inactive,.97),hit(negative,.96),hit(UUID.randomUUID().toString(),.95),hit(good,.80)));
        var result=service.personalized(USER,List.of());
        assertEquals(1,result.products().size()); assertEquals(150,result.products().getFirst().product().price());
        assertEquals(2,result.products().getFirst().product().stockOnHand());
    }
    @Test void cpuCandidatesMustMatchSelectedMainboardSocket() {
        var board=add("mainboard",100,1,0,"ACTIVE","LGA 1700");
        var intel=add("cpu",100,1,0,"ACTIVE","LGA1700");
        var amd=add("cpu",100,1,0,"ACTIVE","AM5");
        var unknown=add("cpu",100,1,0,"ACTIVE",null);
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit(amd,.99),hit(unknown,.98),hit(intel,.8)));
        var result=service.personalized(USER,List.of(board.id()));
        assertEquals(1,result.products().size()); assertEquals(intel.id().toString(),result.products().getFirst().product().id());
        assertEquals("socket_match",result.products().getFirst().compatibilityStatus());
    }
    @Test void incompatibleOrUnknownBuildIsRejectedBeforeEmbedding() {
        var cpu=add("cpu",100,1,0,"ACTIVE","AM5");
        var board=add("mainboard",100,1,0,"ACTIVE","LGA1700");
        assertEquals(400,assertThrows(RecommendationFailure.class,()->service.personalized(USER,List.of(cpu.id(),board.id()))).status);
        var unknown=add("mainboard",100,1,0,"ACTIVE",null);
        assertEquals(400,assertThrows(RecommendationFailure.class,()->service.personalized(USER,List.of(unknown.id()))).status);
        assertEquals(400,assertThrows(RecommendationFailure.class,()->service.personalized(USER,Collections.nCopies(21,cpu.id()))).status);
        verifyNoInteractions(vectors);
    }
    @Test void similarResultsExcludeSourceDuplicatesWrongCategoryAndOutsidePriceBand() {
        var source=add("ram",100,1,0,"ACTIVE",null);
        var low=add("ram",79,1,0,"ACTIVE",null);
        var high=add("ram",121,1,0,"ACTIVE",null);
        var minimum=add("ram",80,1,0,"ACTIVE",null);
        var maximum=add("ram",120,1,0,"ACTIVE",null);
        var other=add("cpu",100,1,0,"ACTIVE","AM5");
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit(source,1.0),hit(low,.99),hit(high,.98),
                hit(other,.97),hit(minimum,.8),hit(minimum,.9),hit(maximum,.85)));
        var result=service.similar(source.id(),List.of());
        assertEquals(List.of(minimum.id().toString(),maximum.id().toString()),result.products().stream().map(r->r.product().id()).toList());
    }
    @Test void cpuReplacementPreservesSourceSocket() {
        var source=add("cpu",100,1,0,"ACTIVE","LGA1700");
        var wrong=add("cpu",100,1,0,"ACTIVE","AM5");
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit(wrong,.99)));
        assertEquals("no_match",service.similar(source.id(),List.of()).status());
    }
    @Test void unknownSourceSocketDoesNotInventAPlatform() {
        var source=add("cpu",100,1,0,"ACTIVE",null);
        assertEquals("no_match",service.similar(source.id(),List.of()).status());
        verifyNoInteractions(vectors);
    }
    @Test void historyIsScopedWhitelistedAndHasNoInventedFallback() {
        when(behavior.topCategories(eq(USER),any(),eq(3))).thenReturn(List.of("laptop","cpu","cpu","mainboard","ram","vga"));
        service.personalized(USER,List.of());
        var request=ArgumentCaptor.forClass(SearchRequest.class); verify(vectors).similaritySearch(request.capture());
        assertFalse(request.getValue().getQuery().contains("laptop"));
        reset(vectors);
        when(behavior.topCategories(eq(USER),any(),eq(3))).thenReturn(List.of());
        assertEquals("insufficient_history",service.personalized(USER,List.of()).status());
        verifyNoInteractions(vectors);
    }
    @Test void providerFailuresAreExplicitAndDoNotLeakDetails() {
        when(vectors.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("secret-provider-message"));
        var error=assertThrows(RecommendationFailure.class,()->service.personalized(USER,List.of()));
        assertEquals(503,error.status); assertFalse(error.getMessage().contains("secret"));
        assertEquals(404,assertThrows(RecommendationFailure.class,()->service.similar(UUID.randomUUID(),List.of())).status);
        assertEquals(401,assertThrows(RecommendationFailure.class,()->service.personalized(null,List.of())).status);
        var missing=new ProductSemanticRecommendationService(catalog,()->null,()->null,Clock.systemUTC(),"test");
        assertEquals("BEHAVIOR_UNAVAILABLE",assertThrows(RecommendationFailure.class,()->missing.personalized(USER,List.of())).code);
        var disabled=new ProductSemanticRecommendationService(catalog,()->null,()->behavior,Clock.systemUTC(),"test");
        assertEquals("VECTOR_UNAVAILABLE",assertThrows(RecommendationFailure.class,()->disabled.personalized(USER,List.of())).code);
    }
    @Test void rereadsContextAndSourceAfterVectorCall() {
        var source=add("cpu",100,1,0,"ACTIVE","LGA1700");
        var candidate=add("cpu",100,1,0,"ACTIVE","LGA1700");
        when(vectors.similaritySearch(any(SearchRequest.class))).thenAnswer(call -> {
            items.put(source.id(),new Item(source.id(),source.product(),"ACTIVE",1,0,"AM5"));
            return List.of(hit(candidate,.99));
        });
        assertTrue(service.similar(source.id(),List.of()).products().isEmpty());
    }
    @Test void resultCountNeverExceedsFive() {
        List<Document> hits=new ArrayList<>();
        for(int i=0;i<8;i++) hits.add(hit(add("cpu",100,1,0,"ACTIVE","AM5"),.8+i*.01));
        when(vectors.similaritySearch(any(SearchRequest.class))).thenReturn(hits);
        assertEquals(5,service.personalized(USER,List.of()).products().size());
    }
}
