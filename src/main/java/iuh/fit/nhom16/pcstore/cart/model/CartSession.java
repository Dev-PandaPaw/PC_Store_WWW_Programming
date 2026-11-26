package iuh.fit.nhom16.pcstore.cart.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class CartSession implements Serializable {
    public static final String ATTRIBUTE = CartSession.class.getName();
    private static final int MAX_TRACKED_OPERATIONS = 200;

    public record AppliedAddOperation(
            UUID operationId,
            UUID productId,
            int quantity,
            long resultVersion) implements Serializable {
        public AppliedAddOperation {
            Objects.requireNonNull(operationId, "operationId");
            Objects.requireNonNull(productId, "productId");
        }
    }

    private long version;
    private boolean buildCheckoutRequired;
    private Map<String, Object> buildRequirements = Map.of();
    private final UUID mergeId;
    private final Map<UUID, Integer> quantities = new LinkedHashMap<>();
    private final Map<UUID, AppliedAddOperation> appliedAdds = new LinkedHashMap<>();
    private final List<AppliedAddOperation> pendingAppliedAdds = new ArrayList<>();

    public CartSession() {
        this(UUID.randomUUID(), 0, Map.of());
    }

    public CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities) {
        this.mergeId = mergeId;
        this.version = version;
        this.quantities.putAll(quantities);
    }

    public CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities, boolean buildCheckoutRequired) {
        this(mergeId, version, quantities);
        this.buildCheckoutRequired = buildCheckoutRequired;
    }

    public CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities,
                       boolean required, Map<String, Object> requirements) {
        this(mergeId, version, quantities, required);
        this.buildRequirements = validatedRequirements(requirements);
    }

    private CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities,
                        boolean required, Map<String, Object> requirements,
                        Map<UUID, AppliedAddOperation> appliedAdds) {
        this(mergeId, version, quantities, required, requirements);
        this.appliedAdds.putAll(appliedAdds);
    }

    public UUID mergeId() {
        return mergeId;
    }

    public CartSession copy() {
        return new CartSession(mergeId, version, quantities, buildCheckoutRequired, buildRequirements, appliedAdds);
    }

    public AppliedAddOperation appliedAdd(UUID operationId) {
        return operationId == null ? null : appliedAdds.get(operationId);
    }

    public void preloadAppliedAdd(AppliedAddOperation operation) {
        if (operation != null) {
            putBoundedOperation(operation);
        }
    }

    public void recordAppliedAdd(AppliedAddOperation operation) {
        Objects.requireNonNull(operation, "operation");
        if (!appliedAdds.containsKey(operation.operationId())) {
            putBoundedOperation(operation);
            pendingAppliedAdds.add(operation);
        }
    }

    private void putBoundedOperation(AppliedAddOperation operation) {
        appliedAdds.put(operation.operationId(), operation);
        while (appliedAdds.size() > MAX_TRACKED_OPERATIONS) {
            UUID oldest = appliedAdds.keySet().iterator().next();
            appliedAdds.remove(oldest);
        }
    }

    public Map<UUID, AppliedAddOperation> appliedAdds() {
        return Collections.unmodifiableMap(appliedAdds);
    }

    public List<AppliedAddOperation> pendingAppliedAdds() {
        return Collections.unmodifiableList(pendingAppliedAdds);
    }

    public void clearPendingAppliedAdds() {
        pendingAppliedAdds.clear();
    }

    public Map<String, Object> buildRequirements() {
        return buildRequirements;
    }

    public void setBuildRequirements(Map<String, Object> requirements) {
        buildRequirements = validatedRequirements(requirements);
        buildCheckoutRequired = true;
    }

    public void mergeBuildRequirements(Map<String, Object> requirements) {
        var next = new LinkedHashMap<>(buildRequirements);
        requirements.forEach((key, value) -> {
            if (key.equals("require_dedicated_gpu")) {
                next.put(key, Boolean.TRUE.equals(next.get(key)) || Boolean.TRUE.equals(value));
            } else {
                next.put(key, Math.max(((Number) next.getOrDefault(key, 0)).intValue(), ((Number) value).intValue()));
            }
        });
        setBuildRequirements(next);
    }

    private static Map<String, Object> validatedRequirements(Map<String, Object> values) {
        var next = new LinkedHashMap<String, Object>();
        for (var entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals("require_dedicated_gpu") && value instanceof Boolean) {
                next.put(key, value);
            } else if ((key.equals("min_ram_gb") || key.equals("min_storage_gb")) && value instanceof Number) {
                int amount = new java.math.BigDecimal(value.toString()).intValueExact();
                if (amount < 1 || amount > (key.equals("min_ram_gb") ? 4096 : 100000)) {
                    throw new IllegalArgumentException("Invalid build capacity");
                }
                next.put(key, amount);
            } else {
                throw new IllegalArgumentException("Invalid build requirement");
            }
        }
        return Map.copyOf(next);
    }

    public boolean buildCheckoutRequired() {
        return buildCheckoutRequired && !quantities.isEmpty();
    }

    public void markBuildCheckout() {
        buildCheckoutRequired = true;
    }

    public void clearBuildCheckout() {
        buildCheckoutRequired = false;
        buildRequirements = Map.of();
    }

    public long version() {
        return version;
    }

    public void incrementVersion() {
        version++;
    }

    public Map<UUID, Integer> quantities() {
        return quantities;
    }
}
