package iuh.fit.nhom16.pcstore.cart.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class CartEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "version", nullable = false)
    private long version = 0L;

    @Column(name = "pc_build_checkout", nullable = false)
    private boolean pcBuildCheckout = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pc_build_requirements", columnDefinition = "jsonb", nullable = false)
    private String pcBuildRequirements = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id.productId ASC")
    private List<CartItemEntity> items = new ArrayList<>();

    public CartEntity() {
    }

    public CartEntity(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
        this.version = 0L;
        this.pcBuildCheckout = false;
        this.pcBuildRequirements = "{}";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void syncItems(Map<UUID, Integer> quantities) {
        items.removeIf(item -> !quantities.containsKey(item.getProductId()));
        Map<UUID, CartItemEntity> existing = new LinkedHashMap<>();
        for (CartItemEntity item : items) {
            existing.put(item.getProductId(), item);
        }
        for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
            CartItemEntity current = existing.get(entry.getKey());
            if (current != null) {
                if (current.getQuantity() != entry.getValue()) {
                    current.setQuantity(entry.getValue());
                }
            } else {
                items.add(new CartItemEntity(this, entry.getKey(), entry.getValue()));
            }
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isPcBuildCheckout() {
        return pcBuildCheckout;
    }

    public void setPcBuildCheckout(boolean pcBuildCheckout) {
        this.pcBuildCheckout = pcBuildCheckout;
    }

    public String getPcBuildRequirements() {
        return pcBuildRequirements;
    }

    public void setPcBuildRequirements(String pcBuildRequirements) {
        this.pcBuildRequirements = pcBuildRequirements;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CartItemEntity> getItems() {
        return items;
    }
}
