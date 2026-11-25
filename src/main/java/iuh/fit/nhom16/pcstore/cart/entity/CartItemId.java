package iuh.fit.nhom16.pcstore.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CartItemId implements Serializable {

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    public CartItemId() {
    }

    public CartItemId(UUID cartId, UUID productId) {
        this.cartId = cartId;
        this.productId = productId;
    }

    public UUID getCartId() {
        return cartId;
    }

    public void setCartId(UUID cartId) {
        this.cartId = cartId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItemId that)) return false;
        return Objects.equals(cartId, that.cartId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId, productId);
    }
}
