package iuh.fit.nhom16.pcstore.ai;

import java.util.List;
import java.util.Map;

public record ProductCriteria(List<String> groups, String purpose, Long minBudget, Long maxBudget,
        boolean inStockOnly, boolean cheaper, boolean cheapest, List<String> keywords) {
    public ProductCriteria(List<String> groups, String purpose, Long minBudget, Long maxBudget,
            boolean inStockOnly, boolean cheaper, List<String> keywords) {
        this(groups,purpose,minBudget,maxBudget,inStockOnly,cheaper,cheaper && minBudget == null && maxBudget == null,keywords);
    }
    public static final Map<String, List<String>> GROUPS = Map.ofEntries(
            Map.entry("pc", List.of("pc-gaming", "pc-office", "pc-workstation")),
            Map.entry("cpu", List.of("cpu")), Map.entry("vga", List.of("vga")),
            Map.entry("mainboard", List.of("mainboard")), Map.entry("ram", List.of("ram")),
            Map.entry("storage", List.of("storage")), Map.entry("cooling", List.of("cooling")),
            Map.entry("case", List.of("case")), Map.entry("psu", List.of("psu")),
            Map.entry("monitor", List.of("monitor")), Map.entry("keyboard", List.of("keyboard")),
            Map.entry("mouse", List.of("mouse")));
    public static ProductCriteria empty() { return new ProductCriteria(List.of(), null, null, null, false, false, List.of()); }
    public boolean valid() {
        return groups != null && groups.size() <= 4 && groups.stream().allMatch(GROUPS::containsKey)
                && (purpose == null || List.of("gaming", "office", "graphics").contains(purpose))
                && (minBudget == null || minBudget >= 0 && minBudget <= 500_000_000)
                && (maxBudget == null || maxBudget >= 0 && maxBudget <= 500_000_000)
                && (minBudget == null || maxBudget == null || minBudget <= maxBudget)
                && keywords != null && keywords.size() <= 12
                && keywords.stream().allMatch(word -> word != null && !word.isBlank() && word.length() <= 80);
    }
}
