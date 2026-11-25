package iuh.fit.nhom16.pcstore.cart.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "cart_items")
public class CartItemEntity {

    @EmbeddedId
    private CartItemId id;

    @Column(name = "id", nullable = false, updatable = false)
    private UUID lineId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cartId")
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public CartItemEntity() {
    }

    public CartItemEntity(CartEntity cart, UUID productId, int quantity) {
        this.cart = cart;
        this.id = new CartItemId(cart.getId(), productId);
        this.lineId = UUID.randomUUID();
        this.quantity = quantity;
    }

    public CartItemId getId() {
        return id;
    }

    public UUID getProductId() {
        return id != null ? id.getProductId() : null;
    }

    public UUID getLineId() {
        return lineId;
    }

    public CartEntity getCart() {
        return cart;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
