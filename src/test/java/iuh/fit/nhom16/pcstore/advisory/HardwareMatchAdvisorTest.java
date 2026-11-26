package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.*;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import iuh.fit.nhom16.pcstore.catalog.application.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static iuh.fit.nhom16.pcstore.advisory.HardwareAttachmentServiceTest.part;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HardwareMatchAdvisorTest {
    StorefrontCatalog storefront=mock(StorefrontCatalog.class);
    RecommendationCatalog catalog=mock(RecommendationCatalog.class);
    ProductRetrievalService retrieval=mock(ProductRetrievalService.class);
    ComponentBuildAdvisor builds=mock(ComponentBuildAdvisor.class);
    AiGateway ai=mock(AiGateway.class);
    HardwareMatchAdvisor service=new HardwareMatchAdvisor(storefront,catalog,retrieval,builds,ai,JsonMapper.builder().build());
    List<Item> items;
    @BeforeEach void setup() {
        items=new ArrayList<>(List.of(ComponentBuildAdvisorTest.item("ram",1_000_000,null,Map.of("memory_type","DDR4","capacity_gb",32)),
                ComponentBuildAdvisorTest.item("ram",1_500_000,null,Map.of("memory_type","DDR5","capacity_gb",32))));
        when(catalog.indexableProducts()).thenAnswer(call->List.copyOf(items));
        when(storefront.products()).thenAnswer(call->items.stream().map(Item::product).toList());
        when(retrieval.rank(anyString(),anyList())).thenAnswer(call->call.getArgument(1));
        when(ai.ask(anyString(),any(),eq(AiAdvisorService.Decision.class),anyString())).thenAnswer(call->new AiAdvisorService.Decision("recommend",List.of(new AiAdvisorService.Recommendation(items.getFirst().product().id(),List.of("compatibility"))),"none"));
    }
    HardwareContext current() { return new HardwareContext("current",List.of(part("mainboard","B760 DDR4","LGA1700","DDR4",0,0),part("ram","Old RAM","","DDR4",16,0)),"specs.png"); }
    AiAdvisorService.ChatResponse advise(HardwareContext context,String message) { return service.advise(context,List.of(message),CriteriaParser.parse(message),"client"); }
    @Test void currentMachineDoesNotForceOldRamModelOrCapacityOntoNewPurchase() {
        var result=advise(current(),"Tìm RAM dưới 2 triệu");
        assertEquals("recommendations",result.outcome());assertEquals(1,result.products().size());
        assertEquals(items.getFirst().id().toString(),result.products().getFirst().get("id"));
        assertNotNull(result.products().getFirst().get("hardwareFit"));
    }
    @Test void incompatibleDdrAndSocketAreExcludedEvenIfModelWantsThem() {
        items.removeFirst();
        assertEquals("no_match",advise(current(),"Tìm RAM dưới 2 triệu").outcome());verifyNoInteractions(ai);
        items=List.of(ComponentBuildAdvisorTest.item("cpu",2_000_000,"AM5",Map.of()));
        assertEquals("no_match",advise(current(),"Tìm CPU dưới 5 triệu").outcome());
    }
    @Test void currentMachinePhotoAutomaticallyFindsRelevantUpgradeParts() {
        var result=advise(current(),"Đề xuất linh kiện phù hợp với thông số trong file.");
        assertEquals("recommendations",result.outcome());
        assertEquals(List.of("ram"),result.criteria().groups());
        assertEquals(items.getFirst().id().toString(),result.products().getFirst().get("id"));
        verify(retrieval).rank(contains("Old RAM"),anyList());
    }
    @Test void automaticUpgradeNeverReducesTheCurrentRamCapacity() {
        var current=new HardwareContext("current",List.of(part("ram","RAM 64GB DDR4","","DDR4",64,0)),"specs.png");
        assertEquals("no_match",advise(current,"Đề xuất linh kiện phù hợp với thông số trong file.").outcome());
        verifyNoInteractions(ai);
    }
    @Test void uncertainExtractionAsksForClearerPhotoWithoutOfferingWrongProducts() {
        var low=new HardwareContext("current",List.of(new HardwareContext.Part("ram","Unclear","","",0,0,"low")),"x.jpg");
        assertEquals("clarification",advise(low,"Tìm RAM dưới 2 triệu").outcome());verifyNoInteractions(retrieval,ai);
    }
    @Test void desiredPhotoMatchesComponentsInsteadOfDefaultingToPrebuiltPc() {
        var desired=new HardwareContext("desired",List.of(part("ram","RAM 32GB DDR4","","DDR4",32,0)),"specs.png");
        var result=advise(desired,"Đề xuất linh kiện phù hợp với thông số trong file.");
        assertEquals("recommendations",result.outcome());assertEquals(List.of("ram"),result.criteria().groups());
    }
    @Test void explicitNewBuildUsesPhotoTargetsWithoutForcingUploadModeChange() {
        var expected=new AiAdvisorService.ChatResponse("Build",List.of(),"recommendations",null,ProductCriteria.empty(),List.of());
        when(builds.advise(anyList(),any(),anyString(),any())).thenReturn(expected);
        var result=advise(current(),"Build PC theo ảnh dưới 30 triệu");
        assertSame(expected,result);verifyNoInteractions(retrieval,ai);
    }
    @Test void desiredMemoryCapacityAndTypeAreHardConstraints() {
        var desired=new HardwareContext("desired",List.of(part("ram","RAM 32GB DDR4","","DDR4",32,0)),"x.png");
        assertEquals("recommendations",advise(desired,"Tìm RAM dưới 2 triệu").outcome());
        desired=new HardwareContext("desired",List.of(part("ram","RAM 64GB DDR4","","DDR4",64,0)),"x.png");
        assertEquals("no_match",advise(desired,"Tìm RAM dưới 2 triệu").outcome());
    }
    @Test void staleSpecsAndForgedSelectionCannotBypassCompatibility() {
        when(ai.ask(anyString(),any(),eq(AiAdvisorService.Decision.class),anyString())).thenReturn(new AiAdvisorService.Decision("recommend",List.of(new AiAdvisorService.Recommendation(items.getLast().product().id(),List.of("compatibility"))),"none"));
        assertEquals("HARDWARE_CATALOG_CHANGED",assertThrows(AiFailure.class,()->advise(current(),"Tìm RAM dưới 2 triệu")).code);
    }
    @Test void requestedUpgradeCapacityDoesNotReuseCurrentCapacityAsTheTarget() {
        assertEquals("recommendations",advise(current(),"Tìm RAM lên 32GB dưới 2 triệu").outcome());
        assertEquals("no_match",advise(current(),"Tìm RAM lên 64GB dưới 2 triệu").outcome());
    }
    @Test void desiredBrandInterfaceAndVisibleDdrMustMatch() {
        var item=ComponentBuildAdvisorTest.item("storage",1_000_000,null,Map.of("capacity_gb",1000,"interface","NVMe"));
        var p=item.product();
        var samsung=new StoreProduct(p.id(),null,p.slug(),"Samsung SSD 1TB NVMe","storage","Samsung","",p.price(),p.price(),List.of(),10,"storage");
        var target=new HardwareContext("desired",List.of(part("storage","Samsung NVMe 1TB","","",1000,0)),"x.png");
        assertTrue(HardwareMatchAdvisor.desiredFits(samsung,item,target));
        target=new HardwareContext("desired",List.of(part("storage","Kingston NVMe 1TB","","",1000,0)),"x.png");
        assertFalse(HardwareMatchAdvisor.desiredFits(samsung,item,target));
        target=new HardwareContext("desired",List.of(part("storage","Samsung SATA 1TB","","",1000,0)),"x.png");
        assertFalse(HardwareMatchAdvisor.desiredFits(samsung,item,target));
        target=new HardwareContext("desired",List.of(part("ram","RAM DDR5 32GB","","",32,0)),"x.png");
        assertFalse(HardwareMatchAdvisor.desiredFits(items.getFirst().product(),items.getFirst(),target));
    }
}
