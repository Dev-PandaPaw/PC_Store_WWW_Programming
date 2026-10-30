package iuh.fit.nhom16.pcstore.ai;

import iuh.fit.nhom16.pcstore.catalog.StoreProduct;
import java.util.*;

public final class ProductMatcher {
    private ProductMatcher() {}
    public static boolean category(StoreProduct product, String category) {
        if (category == null || category.isBlank() || category.equals("ALL")) return true;
        if (product.category().equals(category)) return true;
        List<String> categories = switch (category) {
            case "components" -> List.of("cpu","vga","mainboard","ram","storage","cooling","case","psu");
            case "memory" -> List.of("ram","storage"); case "build" -> List.of("cooling","case","psu");
            case "gear" -> List.of("mouse","keyboard");
            default -> ProductCriteria.GROUPS.getOrDefault(category,List.of());
        };
        return categories.contains(product.category());
    }
    public static boolean matches(StoreProduct product, ProductCriteria criteria) {
        if (!criteria.groups().isEmpty() && criteria.groups().stream().noneMatch(group -> category(product,group))) return false;
        if (criteria.groups().contains("pc") && criteria.purpose() != null && product.category().startsWith("pc-")) {
            String expected = switch (criteria.purpose()) { case "gaming" -> "pc-gaming"; case "office" -> "pc-office"; default -> "pc-workstation"; };
            if (!product.category().equals(expected)) return false;
        }
        if (criteria.inStockOnly() && product.stockOnHand() <= 0) return false;
        if (criteria.maxBudget() != null && product.price() > criteria.maxBudget()) return false;
        if (criteria.minBudget() != null && product.price() < criteria.minBudget()) return false;
        String text = CriteriaParser.normalize(product.searchable()).replaceAll("[\\s-]+", "");
        return criteria.keywords().stream().allMatch(word -> {
            String keyword = CriteriaParser.normalize(word).replaceAll("[\\s-]+", "");
            return switch (keyword) {
                case "wireless" -> text.matches(".*(wireless|khongday|bluetooth).*");
                case "white" -> text.contains("white") || text.contains("trang");
                default -> text.contains(keyword);
            };
        });
    }
    public static List<StoreProduct> candidates(List<StoreProduct> products, ProductCriteria criteria) {
        List<StoreProduct> pool = products.stream().filter(p -> matches(p,criteria)).toList();
        pool = cheapest(pool, criteria);
        Map<String,Long> cheapest = new HashMap<>();
        for (String group : criteria.groups()) {
            long price = pool.stream().filter(p -> category(p,group)).mapToLong(StoreProduct::price).min().orElse(Long.MAX_VALUE/8);
            cheapest.put(group,price);
        }
        long minimum = cheapest.values().stream().mapToLong(Long::longValue).sum();
        if (criteria.groups().size() > 1 && criteria.maxBudget() != null && minimum > criteria.maxBudget()) return List.of();
        Comparator<StoreProduct> order = Comparator.comparingLong(StoreProduct::price);
        if (!criteria.cheaper() && criteria.maxBudget() != null) order = order.reversed();
        List<StoreProduct> selected = new ArrayList<>();
        for (String group : criteria.groups().stream().limit(4).toList()) {
            final long reserve = minimum - cheapest.get(group);
            selected.addAll(pool.stream().filter(p -> category(p,group))
                    .filter(p -> criteria.groups().size() <= 1 || criteria.maxBudget() == null || p.price()+reserve <= criteria.maxBudget())
                    .sorted(order).limit(Math.max(2,8/Math.max(1,criteria.groups().size()))).toList());
        }
        return selected.stream().distinct().limit(8).toList();
    }
    /** Apply only after suitability, user filters and stock checks. Keep all exact-price ties. */
    public static List<StoreProduct> cheapest(List<StoreProduct> products, ProductCriteria criteria) {
        if (!criteria.cheapest() || products.isEmpty()) return products;
        Map<String,Long> prices = new HashMap<>();
        for (StoreProduct product : products) {
            String group = criteria.groups().stream().filter(g -> category(product,g)).findFirst().orElse("all");
            prices.merge(group,product.price(),Math::min);
        }
        return products.stream().filter(product -> {
            String group = criteria.groups().stream().filter(g -> category(product,g)).findFirst().orElse("all");
            return product.price() == prices.get(group);
        }).toList();
    }
}
