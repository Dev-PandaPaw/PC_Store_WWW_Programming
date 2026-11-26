package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.ai.*;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import iuh.fit.nhom16.pcstore.catalog.application.ProductRetrievalService;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComponentBuildAdvisorTest {
    RecommendationCatalog catalog=mock(RecommendationCatalog.class);
    ProductRetrievalService retrieval=mock(ProductRetrievalService.class);
    AiGateway ai=mock(AiGateway.class);
    ComponentBuildAdvisor advisor=new ComponentBuildAdvisor(catalog,retrieval,ai,JsonMapper.builder().build());
    List<Item> products;
    @BeforeEach void setup() {
        products=new ArrayList<>(fixtures());
        when(catalog.indexableProducts()).thenAnswer(call->List.copyOf(products));
        when(catalog.findAll(anySet())).thenAnswer(call->products.stream().collect(Collectors.toMap(Item::id,i->i)));
        when(retrieval.rank(anyString(),anyList())).thenAnswer(call->call.getArgument(1));
        when(ai.ask(anyString(),any(),eq(ComponentBuildAdvisor.BuildChoice.class),anyString()))
                .thenReturn(new ComponentBuildAdvisor.BuildChoice("build-0"));
        when(ai.model()).thenReturn("fixture");
    }
    static List<Item> fixtures() {
        return List.of(item("cpu",2_000_000,"LGA1700",Map.of()),item("mainboard",2_000_000,"LGA1700",Map.of("ram_type","DDR4")),
                item("ram",1_000_000,null,Map.of("memory_type","DDR4","capacity_gb",16)),item("vga",4_000_000,null,Map.of()),
                item("storage",1_000_000,null,Map.of("capacity_gb",1000)),item("psu",1_000_000,null,Map.of("wattage_w",650)),
                item("case",1_000_000,null,Map.of()),item("cooling",500_000,null,Map.of()));
    }
    static Item item(String group,long price,String socket,Map<String,Object> specs) {
        UUID id=UUID.randomUUID();
        var product=new StoreProduct(id.toString(),null,group+"-"+id,group.equals("cpu")?"Intel CPU":group,
                group,"brand","",price,price,List.of(new StoreProduct.Spec("catalog spec")),10,group);
        return new Item(id,product,"ACTIVE",10,0,socket,specs,"draft");
    }
    AiAdvisorService.ChatResponse build(String message) { return advisor.advise(List.of(message),CriteriaParser.parse(message),"client"); }
    static Item named(Item item,String name) {
        var p=item.product();
        var named=new StoreProduct(p.id(),p.catalogKey(),p.slug(),name,p.category(),p.brand(),p.description(),p.price(),p.originalPrice(),p.specs(),p.stockOnHand(),p.image());
        return new Item(item.id(),named,item.status(),item.stockOnHand(),item.reservedQuantity(),item.socket(),item.componentSpecs(),item.verificationStatus());
    }
    @Test void exactCpuAndGpuModelsDoNotMatchLongerSuffixVariants() {
        products.removeIf(i->Set.of("cpu","vga").contains(i.product().category()));
        products.add(named(item("cpu",2_000_000,"LGA1700",Map.of()),"Intel Core i5-12400F"));
        products.add(named(item("cpu",2_100_000,"LGA1700",Map.of()),"Intel Core i5-12400"));
        products.add(named(item("vga",4_000_000,null,Map.of()),"MSI GeForce RTX 5060 Ti"));
        products.add(named(item("vga",4_100_000,null,Map.of()),"Zotac GeForce RTX 5060"));
        var result=build("Build PC Intel Core i5-12400 và RTX 5060 dưới 20 triệu");
        assertEquals("recommendations",result.outcome());
        assertEquals("Intel Core i5-12400",result.products().getFirst().get("name"));
        assertEquals("Zotac GeForce RTX 5060",result.products().get(3).get("name"));
    }
    @Test void choosesBuildWithEvidenceBeforeEquallyPricedProvisionalCooler() throws Exception {
        BuildCompatibilityTest.load();
        products=new ArrayList<>(BuildCompatibilityTest.INTEL.stream().map(BuildCompatibilityTest.catalog::get).toList());
        products.addFirst(BuildCompatibilityTest.catalog.get("cooler-master-hyper-212-spectrum-v3"));
        var result=build("Build PC Intel DDR4 dưới 20 triệu");
        assertEquals("compatible",result.build().status());
        assertEquals(BuildCompatibilityTest.catalog.get("deepcool-ag400-argb").id().toString(),result.build().items().getLast().productId());
    }
    @Test void completeBuildContainsExactlyEightSeparateGroupsAndCorrectTotal() {
        var result=build("Build PC linh kiện rời dưới 20 triệu");
        assertEquals("recommendations",result.outcome()); assertEquals(8,result.products().size());
        assertEquals(12_500_000,result.build().total()); assertEquals("vector",result.build().retrieval());
        assertEquals(8,result.products().stream().map(p->p.get("category")).distinct().count());
        assertEquals("needs_verification",result.build().status());
        assertTrue(result.build().checks().stream().allMatch(c->c.status().equals("needs_verification")));
        assertTrue(result.products().stream().noneMatch(p->p.get("category").toString().startsWith("pc-")));
    }
    @Test void missingBudgetClarifiesBeforeEmbeddingOrModelCall() {
        assertEquals("clarification",build("Build PC linh kiện rời").outcome());
        verifyNoInteractions(retrieval,ai,catalog);
    }
    @Test void separateComponentCorrectionAndShortReplyKeepTheBuildIntent() {
        assertTrue(ComponentBuildAdvisor.requested(List.of("PC dưới 30 triệu", "Từng linh kiện chứ không phải 1 bộ mẫu sẵn", "hiệu năng")));
        assertTrue(ComponentBuildAdvisor.requested(List.of("Không phải PC bộ, mình muốn từng linh kiện")));
        assertFalse(ComponentBuildAdvisor.requested(List.of("Build PC dưới 30 triệu", "Không muốn build PC nữa, tìm chuột")));
        assertFalse(ComponentBuildAdvisor.requested(List.of("Build PC dưới 30 triệu", "Tìm PC bộ lắp sẵn")));
    }
    @Test void missingCategoryAndLowBudgetNeverReturnPartialPc() {
        products.removeIf(p->p.product().category().equals("psu"));
        var missing=build("Build PC dưới 20 triệu");
        assertEquals(List.of("Nguồn"),missing.build().missingCategories()); assertTrue(missing.products().isEmpty());
        products=new ArrayList<>(fixtures());
        var low=build("Build PC dưới 5 triệu");
        assertEquals("no_match",low.outcome()); assertTrue(low.products().isEmpty()); verifyNoInteractions(ai);
    }
    @Test void rejectsSocketAndDdrMismatchDespiteDraftData() {
        products.removeIf(p->p.product().category().equals("mainboard"));
        products.add(item("mainboard",1_000_000,"AM5",Map.of("ram_type","DDR4")));
        assertEquals("no_match",build("Build PC dưới 20 triệu").outcome());
        products.removeIf(p->p.product().category().equals("mainboard"));
        products.add(item("mainboard",1_000_000,"LGA1700",Map.of("ram_type","DDR5")));
        assertEquals("no_match",build("Build PC dưới 20 triệu").outcome()); verifyNoInteractions(ai);
    }
    @Test void filtersReservedStockAndExplicitCpuBrand() {
        assertEquals("no_match",build("Build PC AMD dưới 20 triệu").outcome());
        var cpu=products.removeFirst();
        products.add(new Item(cpu.id(),cpu.product(),"ACTIVE",10,10,cpu.socket(),cpu.componentSpecs(),"verified"));
        assertEquals("no_match",build("Build PC dưới 20 triệu").outcome());
    }
    @Test void rejectsKnownGpuPowerAndLengthConflicts() {
        products.removeIf(p->p.product().category().equals("vga"));
        products.add(item("vga",4_000_000,null,Map.of("recommended_psu_w",750)));
        assertEquals("no_match",build("Build PC dưới 20 triệu").outcome());
        products.removeIf(p->Set.of("vga","case").contains(p.product().category()));
        products.add(item("vga",4_000_000,null,Map.of("length_mm",350)));
        products.add(item("case",1_000_000,null,Map.of("max_gpu_length_mm",300)));
        assertEquals("no_match",build("Build PC dưới 20 triệu").outcome());
    }
    @Test void forgedModelOptionAndChangedPriceAreRejected() {
        when(ai.ask(anyString(),any(),eq(ComponentBuildAdvisor.BuildChoice.class),anyString()))
                .thenReturn(new ComponentBuildAdvisor.BuildChoice("forged"));
        assertEquals("AI_INVALID_BUILD",assertThrows(AiFailure.class,()->build("Build PC dưới 20 triệu")).code);
        when(ai.ask(anyString(),any(),eq(ComponentBuildAdvisor.BuildChoice.class),anyString()))
                .thenReturn(new ComponentBuildAdvisor.BuildChoice("build-0"));
        when(catalog.findAll(anySet())).thenReturn(Map.of());
        assertEquals("BUILD_CATALOG_CHANGED",assertThrows(AiFailure.class,()->build("Build PC dưới 20 triệu")).code);
    }
    @Test void vectorFailureDoesNotPretendToBeAnEmbeddedBuild() {
        when(retrieval.rank(anyString(),anyList())).thenThrow(new AiFailure(503,"VECTOR_INDEX_EMPTY","empty"));
        assertEquals("VECTOR_INDEX_EMPTY",assertThrows(AiFailure.class,()->build("Build PC dưới 20 triệu")).code);
        verifyNoInteractions(ai);
    }
    @Test void priceAndStockAreReadAgainAfterModelSelection() {
        var changed=new HashMap<UUID,Item>(); products.forEach(item->changed.put(item.id(),item));
        var cpu=products.getFirst(); var old=cpu.product();
        var expensive=new StoreProduct(old.id(),old.catalogKey(),old.slug(),old.name(),old.category(),old.brand(),old.description(),
                30_000_000,30_000_000,old.specs(),old.stockOnHand(),old.image());
        changed.put(cpu.id(),new Item(cpu.id(),expensive,"ACTIVE",10,0,cpu.socket(),cpu.componentSpecs(),"draft"));
        when(catalog.findAll(anySet())).thenReturn(changed);
        assertEquals("BUILD_CATALOG_CHANGED",assertThrows(AiFailure.class,()->build("Build PC dưới 20 triệu")).code);
        changed.put(cpu.id(),new Item(cpu.id(),old,"ACTIVE",10,10,cpu.socket(),cpu.componentSpecs(),"draft"));
        assertEquals("BUILD_CATALOG_CHANGED",assertThrows(AiFailure.class,()->build("Build PC dưới 20 triệu")).code);
    }
    @Test void buildContextSurvivesBudgetFollowUpAndResetsForPrebuiltOrMouse() {
        assertTrue(ComponentBuildAdvisor.requested(List.of("Build PC linh kiện rời","Dưới 30 triệu")));
        assertTrue(ComponentBuildAdvisor.requested(List.of("Build PC chơi Valorant dưới 30 triệu")));
        assertFalse(ComponentBuildAdvisor.requested(List.of("Build PC linh kiện rời","Tìm PC bộ lắp sẵn")));
        assertFalse(ComponentBuildAdvisor.requested(List.of("Build PC linh kiện rời","Chuột dưới 1 triệu")));
    }
}
