package iuh.fit.nhom16.pcstore.cart.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Cart aggregate used by both session and database storage; never reserves inventory. */
public final class CartSession implements Serializable {
    public static final String ATTRIBUTE = CartSession.class.getName();

    private long version;
    private final UUID mergeId;
    private final Map<UUID, Integer> quantities = new LinkedHashMap<>();

    public CartSession() { this(UUID.randomUUID(), 0, Map.of()); }

    public CartSession(UUID mergeId, long version, Map<UUID, Integer> quantities) {
        this.mergeId = mergeId;
        this.version = version;
        this.quantities.putAll(quantities);
    }

    public UUID mergeId() { return mergeId; }
    public CartSession copy() { return new CartSession(mergeId, version, quantities); }

    public long version() { return version; }
    public void incrementVersion() { version++; }
    public Map<UUID, Integer> quantities() { return quantities; }
}
