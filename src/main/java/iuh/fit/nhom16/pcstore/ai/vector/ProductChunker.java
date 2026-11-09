package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.domain.ProductSpec;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Chunk theo sản phẩm; mô tả dài được chia nhỏ và lặp lại ngữ cảnh linh kiện. */
@Component
public class ProductChunker {
    static final int DESCRIPTION_CHUNK_SIZE = 1200;

    public List<Document> chunk(Product product) {
        Objects.requireNonNull(product.getId(), "Product must have a database ID");
        StringBuilder context = new StringBuilder();
        field(context, "Sản phẩm", product.getName());
        field(context, "SKU", product.getSku());
        if (product.getCategory() != null) field(context, "Danh mục", product.getCategory().getName());
        field(context, "Thương hiệu", product.getBrand());
        ProductSpec spec = product.getSpec();
        if (spec != null) {
            field(context, "CPU", spec.getCpu());
            field(context, "GPU", spec.getGpu());
            if (spec.getRamGb() != null) field(context, "RAM", spec.getRamGb() + " GB");
            if (spec.getStorageGb() != null) field(context, "Dung lượng lưu trữ", spec.getStorageGb() + " GB");
            field(context, "Loại ổ cứng", spec.getStorageType());
            field(context, "Mainboard", spec.getMotherboard());
            field(context, "Nguồn PSU", spec.getPsu());
            field(context, "Vỏ case", spec.getCaseName());
            field(context, "Hệ điều hành", spec.getOperatingSystem());
        }
        String description = clean(product.getDescription());
        List<String> parts = splitDescription(description);
        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < parts.size(); index++) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("productId", product.getId().toString());
            // Numeric types để phía 3.4 có thể filter theo giá/tồn kho.
            metadata.put("price", Objects.requireNonNull(product.getPrice(), "Product price is required").doubleValue());
            metadata.put("category", product.getCategory() == null ? "" : clean(product.getCategory().getSlug()));
            metadata.put("stockOnHand", Objects.requireNonNullElse(product.getStockOnHand(), 0));
            int reserved = Objects.requireNonNullElse(product.getReservedQuantity(), 0);
            metadata.put("availableStock", Math.max(0, (int) metadata.get("stockOnHand") - reserved));
            metadata.put("status", clean(product.getStatus()));
            metadata.put("chunkIndex", index);
            metadata.put("chunkCount", parts.size());
            String id = UUID.nameUUIDFromBytes(("product:" + product.getId() + ":chunk:" + index)
                    .getBytes(StandardCharsets.UTF_8)).toString();
            String text = context + (parts.get(index).isEmpty() ? "" : "Mô tả: " + parts.get(index));
            documents.add(new Document(id, text.strip(), metadata));
        }
        return documents;
    }

    private static List<String> splitDescription(String text) {
        if (text.isEmpty()) return List.of("");
        List<String> parts = new ArrayList<>();
        for (int start = 0; start < text.length();) {
            int end = Math.min(start + DESCRIPTION_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > start) end = boundary;
                // Không cắt giữa surrogate pair (emoji/ký tự Unicode ngoài BMP).
                if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
            }
            parts.add(text.substring(start, end).strip());
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') start++;
        }
        return parts;
    }

    private static void field(StringBuilder text, String label, String value) {
        String cleaned = clean(value);
        if (!cleaned.isEmpty()) text.append(label).append(": ").append(cleaned).append('\n');
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("(?s)<[^>]*>", " ").replaceAll("\\s+", " ").strip();
    }
}
