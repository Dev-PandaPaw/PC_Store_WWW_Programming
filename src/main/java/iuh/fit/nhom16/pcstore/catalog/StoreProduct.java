package iuh.fit.nhom16.pcstore.catalog;

import java.util.List;

public record StoreProduct(String id, String catalogKey, String slug, String name, String category,
        String brand, String description, long price, long originalPrice, List<Spec> specs,
        int stockOnHand, String image) {
    public record Spec(String label) {}
    public String searchable() {
        return name + " " + brand + " " + description + " "
                + String.join(" ", specs.stream().map(Spec::label).toList());
    }
}
