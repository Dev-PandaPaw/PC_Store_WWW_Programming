package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.catalog.domain.Category;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.domain.ProductSpec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductChunkerTest {
    static Product cpu() {
        var category = new Category(); category.setName("CPU"); category.setSlug("cpu");
        var product = new Product();
        product.setId(UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
        product.setName("Intel Core i5 12400F"); product.setSku("CPU-I5-12400F");
        product.setCategory(category); product.setBrand("Intel");
        product.setPrice(new BigDecimal("3200000.00")); product.setStockOnHand(8); product.setReservedQuantity(2);
        product.setDescription("<p>Socket LGA1700, 6 nhân 12 luồng, boost 4.4GHz, TDP 65W.</p> Phù hợp chơi game.");
        var spec = new ProductSpec();
        spec.setCpu("Intel Core i5 12400F"); spec.setMotherboard("H610/B760 LGA1700");
        spec.setGpu("RTX 4060"); spec.setRamGb(16); spec.setStorageGb(512); spec.setStorageType("SSD");
        spec.setPsu("650W"); spec.setCaseName("ATX"); spec.setOperatingSystem("Windows 11");
        product.setSpec(spec);
        return product;
    }

    @Test void preservesRealSpecsAndTypedMetadata() {
        var doc = new ProductChunker().chunk(cpu()).getFirst();
        for (String expected : new String[]{"Sản phẩm: Intel Core i5 12400F", "Danh mục: CPU", "Thương hiệu: Intel",
                "LGA1700", "6 nhân 12 luồng", "4.4GHz", "65W", "RAM: 16 GB", "512 GB", "SSD", "650W", "ATX", "Windows 11"})
            assertTrue(doc.getText().contains(expected), expected);
        assertFalse(doc.getText().contains("<p>"));
        assertFalse(doc.getText().contains("3200000"), "Giá thuộc metadata, không phải nội dung embedding");
        assertEquals(cpu().getId().toString(), doc.getMetadata().get("productId"));
        assertEquals(3200000.0, doc.getMetadata().get("price"));
        assertEquals("cpu", doc.getMetadata().get("category"));
        assertEquals(8, doc.getMetadata().get("stockOnHand"));
        assertEquals(6, doc.getMetadata().get("availableStock"));
    }

    @Test void longDescriptionsKeepEveryWordAndContextWithStableIds() {
        var product = cpu();
        product.setDescription("socket🔧 LGA1700 chơi game ".repeat(190));
        var chunker = new ProductChunker(); var docs = chunker.chunk(product);
        assertTrue(docs.size() > 1);
        assertEquals(docs.size(), docs.stream().map(d -> d.getId()).distinct().count());
        assertEquals(docs.stream().map(d -> d.getId()).toList(), chunker.chunk(product).stream().map(d -> d.getId()).toList());
        String joined = String.join(" ", docs.stream().map(d -> d.getText().split("Mô tả: ", 2)[1]).toList());
        assertEquals(product.getDescription().strip(), joined);
        for (var doc : docs) {
            assertTrue(doc.getText().startsWith("Sản phẩm: Intel"));
            assertEquals(docs.size(), doc.getMetadata().get("chunkCount"));
            assertTrue(doc.getText().split("Mô tả: ", 2)[1].length() <= ProductChunker.DESCRIPTION_CHUNK_SIZE);
        }
    }

    @Test void missingOptionalFieldsNeverProduceInventedSpecsOrNullText() {
        var product = cpu(); product.setSpec(null); product.setDescription(null); product.setBrand(null);
        product.setCategory(null); product.setStockOnHand(0); product.setReservedQuantity(3);
        var doc = new ProductChunker().chunk(product).getFirst();
        assertFalse(doc.getText().contains("null")); assertFalse(doc.getText().contains("LGA1700"));
        assertEquals("", doc.getMetadata().get("category"));
        assertEquals(0, doc.getMetadata().get("availableStock"));
    }
}
