package iuh.fit.nhom16.pcstore.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cart_add_operations")
public class CartAddOperationEntity {

    @EmbeddedId
    private CartAddOperationId id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "result_version", nullable = false)
    private long resultVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CartAddOperationEntity() {
    }

    public CartAddOperationEntity(UUID cartId, UUID operationId, UUID productId, int quantity, long resultVersion) {
        this.id = new CartAddOperationId(cartId, operationId);
        this.productId = productId;
        this.quantity = quantity;
        this.resultVersion = resultVersion;
        this.createdAt = OffsetDateTime.now();
    }

    public CartAddOperationId getId() {
        return id;
    }

    public UUID getOperationId() {
        return id != null ? id.getOperationId() : null;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getResultVersion() {
        return resultVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
