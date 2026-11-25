package iuh.fit.nhom16.pcstore.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cart_merge_receipts")
public class CartMergeReceiptEntity {

    @Id
    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CartMergeReceiptEntity() {
    }

    public CartMergeReceiptEntity(UUID guestId, UUID cartId) {
        this.guestId = guestId;
        this.cartId = cartId;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getGuestId() {
        return guestId;
    }

    public UUID getCartId() {
        return cartId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
