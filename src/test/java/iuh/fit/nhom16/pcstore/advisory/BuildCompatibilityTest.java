package iuh.fit.nhom16.pcstore.advisory;

import iuh.fit.nhom16.pcstore.advisory.domain.BuildCompatibility;
import iuh.fit.nhom16.pcstore.catalog.api.RecommendationCatalog.Item;
import iuh.fit.nhom16.pcstore.catalog.api.dto.StoreProduct;
import org.junit.jupiter.api.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BuildCompatibilityTest {
    static Map<String,Item> catalog;
    static final List<String> INTEL=List.of("intel-core-i3-12100f","msi-pro-h610m-g-ddr4","kingston-fury-beast-16gb-ddr4",
            "sapphire-pulse-rx-6600","crucial-p3-plus-1000gb","msi-mag-a650bn","montech-air-100-argb-black","deepcool-ag400-argb");
    @BeforeAll static void load() throws Exception {
        try(var stream=BuildCompatibilityTest.class.getResourceAsStream("/catalog/build-catalog-2026-11.json")) {
            var rows=new ArrayList<>(JsonMapper.builder().build().readValue(stream,new TypeReference<List<Map<String,Object>>>() {}));
            try(var extension=BuildCompatibilityTest.class.getResourceAsStream("/catalog/build-catalog-ddr5-extension.json")) {
                rows.addAll(JsonMapper.builder().build().readValue(extension,new TypeReference<List<Map<String,Object>>>() {}));
            }
            catalog=new HashMap<>();
            for(var row:rows) {
                String slug=row.get("slug").toString();
                @SuppressWarnings("unchecked") Map<String,Object> facts=(Map<String,Object>)row.get("facts");
                UUID id=UUID.nameUUIDFromBytes(slug.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var p=new StoreProduct(id.toString(),slug,slug,row.get("name").toString(),row.get("category").toString(),
                        row.get("brand").toString(),"",((Number)row.get("price")).longValue(),0,List.of(),20,"");
                catalog.put(slug,new Item(id,p,"ACTIVE",20,0,Objects.toString(facts.get("socket"),null),facts,row.get("status").toString()));
            }
            assertEquals(78,catalog.size());
            assertEquals(34,rows.stream().filter(r->Boolean.TRUE.equals(r.get("new"))).count());
        }
    }
    List<Item> build(List<String> names) { return names.stream().map(catalog::get).toList(); }
    List<Item> replace(List<Item> build,String category,String slug) {
        return build.stream().map(i->i.product().category().equals(category)?catalog.get(slug):i).toList();
    }
    @Test void verifiedIntelDdr4BuildPassesAllSixScopedChecks() {
        var build=build(INTEL);
        assertTrue(BuildCompatibility.valid(build));
        assertTrue(BuildCompatibility.checks(build).stream().allMatch(c->c.status().equals("passed")));
    }
    @Test void verifiedAm5Ddr5BuildAndAm4Ddr4BuildAreAvailable() {
        var build=build(List.of("amd-ryzen-5-7600","msi-b650-gaming-plus-wifi","kingston-fury-beast-32gb-ddr5",
                "msi-rtx-4060-ventus-2x-black","kingston-nv3-2tb","msi-mag-a750gl-pcie5","corsair-frame-4000d-black","deepcool-ag400-argb"));
        assertTrue(BuildCompatibility.valid(replace(build,"ram","kingston-fury-beast-64gb-ddr5")));
        assertTrue(BuildCompatibility.valid(replace(build,"ram","kingston-fury-beast-16gb-ddr5")));
        assertTrue(BuildCompatibility.valid(build));
        assertTrue(BuildCompatibility.checks(build).stream().allMatch(c->c.status().equals("passed")));
        build=replace(replace(replace(build,"cpu","amd-ryzen-5-5600"),"mainboard","msi-b550m-pro-vdh-wifi"),"ram","kingston-fury-beast-32gb-ddr4");
        assertTrue(BuildCompatibility.valid(build));
        assertTrue(BuildCompatibility.checks(build).stream().allMatch(c->c.status().equals("passed")));
    }
    @Test void rejectsKnownDdrSocketFormFactorAndPowerCableConflicts() {
        var build=build(INTEL);
        assertFalse(BuildCompatibility.valid(replace(build,"ram","kingston-fury-beast-32gb-ddr5")));
        assertFalse(BuildCompatibility.valid(replace(build,"cpu","amd-ryzen-5-7600")));
        assertFalse(BuildCompatibility.valid(replace(replace(replace(build,"cpu","amd-ryzen-5-7600"),"mainboard","msi-b650-gaming-plus-wifi"),"ram","kingston-fury-beast-32gb-ddr5")));
        assertFalse(BuildCompatibility.valid(replace(build,"vga","zotac-rtx-5070-solid-oc"))); // no native 16-pin
        assertFalse(BuildCompatibility.valid(replace(build,"vga","vga-asus-rog-strix-rtx-4080-super-16gb")));
    }
    @Test void unknownLegacyRamDoesNotInheritVerificationFromCpu() {
        var build=replace(build(INTEL),"ram","kingmax-blade-x-16gb-ddr4");
        assertTrue(BuildCompatibility.valid(build));
        assertEquals("needs_verification",BuildCompatibility.checks(build).get(1).status());
    }
    @Test void aioNominalSizeDoesNotPretendToVerifyRadiatorClearance() {
        var build=replace(replace(build(INTEL),"cooling","corsair-nautilus-360-argb-black"),"case","corsair-frame-4000d-black");
        assertTrue(BuildCompatibility.valid(build));
        assertEquals("needs_verification",BuildCompatibility.checks(build).get(4).status());
    }
    @Test void pcieGen5SsdWorksAtLowerGenerationInsteadOfBeingRejected() {
        var build=replace(build(INTEL),"storage","samsung-9100-pro-1tb");
        assertTrue(BuildCompatibility.valid(build));
        assertEquals("passed",BuildCompatibility.checks(build).get(5).status());
    }
    @Test void verifiedStatusDoesNotPromoteMissingOrMalformedFields() {
        var build=new ArrayList<>(build(INTEL));
        Item ram=build.get(2);var facts=new HashMap<>(ram.componentSpecs());facts.remove("verified_fields");
        build.set(2,new Item(ram.id(),ram.product(),ram.status(),20,0,ram.socket(),facts,"verified"));
        assertEquals("needs_verification",BuildCompatibility.checks(build).get(1).status());
        Item psu=build.get(5);facts=new HashMap<>(psu.componentSpecs());facts.put("wattage_w","NaN");
        build.set(5,new Item(psu.id(),psu.product(),psu.status(),20,0,psu.socket(),facts,"verified"));
        assertEquals("needs_verification",BuildCompatibility.checks(build).get(2).status());
    }
}
