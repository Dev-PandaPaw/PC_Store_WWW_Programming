package iuh.fit.nhom16.pcstore.cart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CartAddOperationId implements Serializable {

    @Column(name = "cart_id", nullable = false)
    private UUID cartId;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    public CartAddOperationId() {
    }

    public CartAddOperationId(UUID cartId, UUID operationId) {
        this.cartId = cartId;
        this.operationId = operationId;
    }

    public UUID getCartId() {
        return cartId;
    }

    public UUID getOperationId() {
        return operationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartAddOperationId that)) return false;
        return Objects.equals(cartId, that.cartId) && Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartId, operationId);
    }
}
