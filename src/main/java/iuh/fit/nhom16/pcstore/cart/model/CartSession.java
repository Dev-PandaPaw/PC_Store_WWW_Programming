package iuh.fit.nhom16.pcstore.cart.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


public final class CartSession implements Serializable {
    public static final String ATTRIBUTE = CartSession.class.getName();

    private long version;
    private boolean buildCheckoutRequired;
    private Map<String,Object> buildRequirements = Map.of();
    private final UUID mergeId;
    private final Map<UUID, Integer> quantities = new LinkedHashMap<>();

    public CartSession() { this(UUID.randomUUID(), 0, Map.of()); }

    public CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities) {
        this.mergeId = mergeId;
        this.version = version;
        this.quantities.putAll(quantities);
    }

    public UUID mergeId() { return mergeId; }
    public CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities, boolean buildCheckoutRequired) {
        this(mergeId, version, quantities);
        this.buildCheckoutRequired = buildCheckoutRequired;
    }
    public CartSession(UUID mergeId, long version, Map<UUID,Integer> quantities, boolean required, Map<String,Object> requirements) {
        this(mergeId, version, quantities, required);
        this.buildRequirements = validatedRequirements(requirements);
    }
    public CartSession copy() { return new CartSession(mergeId, version, quantities, buildCheckoutRequired, buildRequirements); }
    public Map<String,Object> buildRequirements() { return buildRequirements; }
    public void setBuildRequirements(Map<String,Object> requirements) {
        buildRequirements = validatedRequirements(requirements);
        buildCheckoutRequired = true;
    }
    public void mergeBuildRequirements(Map<String,Object> requirements) {
        var next = new LinkedHashMap<>(buildRequirements);
        requirements.forEach((key, value) -> {
            if (key.equals("require_dedicated_gpu")) next.put(key, Boolean.TRUE.equals(next.get(key)) || Boolean.TRUE.equals(value));
            else next.put(key, Math.max(((Number)next.getOrDefault(key, 0)).intValue(), ((Number)value).intValue()));
        });
        setBuildRequirements(next);
    }
    private static Map<String,Object> validatedRequirements(Map<String,Object> values) {
        var next = new LinkedHashMap<String,Object>();
        for (var entry : values.entrySet()) {
            String key = entry.getKey(); Object value = entry.getValue();
            if (key.equals("require_dedicated_gpu") && value instanceof Boolean) next.put(key, value);
            else if ((key.equals("min_ram_gb") || key.equals("min_storage_gb")) && value instanceof Number) {
                int amount = new java.math.BigDecimal(value.toString()).intValueExact();
                if (amount < 1 || amount > (key.equals("min_ram_gb") ? 4096 : 100000)) throw new IllegalArgumentException("Invalid build capacity");
                next.put(key, amount);
            } else throw new IllegalArgumentException("Invalid build requirement");
        }
        return Map.copyOf(next);
    }
    public boolean buildCheckoutRequired() { return buildCheckoutRequired && !quantities.isEmpty(); }
    public void markBuildCheckout() { buildCheckoutRequired = true; }
    public void clearBuildCheckout() { buildCheckoutRequired = false; buildRequirements = Map.of(); }

    public long version() { return version; }
    public void incrementVersion() { version++; }
    public Map<UUID, Integer> quantities() { return quantities; }
}
