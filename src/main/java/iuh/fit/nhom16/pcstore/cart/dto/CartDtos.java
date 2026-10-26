package iuh.fit.nhom16.pcstore.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CartDtos {
    private CartDtos() { }

    public record AddItemRequest(
            @NotNull UUID productId,
            @NotNull @Positive @Max(99) Integer quantity,
            @NotNull @Min(0) Long cartVersion) { }

    public record UpdateItemRequest(
            @NotNull @Positive @Max(99) Integer quantity,
            @NotNull @Min(0) Long cartVersion) { }

    public record CartItemResponse(
            UUID productId,
            String sku,
            String name,
            BigDecimal unitPrice,
            int quantity,
            int availableQuantity,
            boolean purchasable) { }

    public record CartResponse(long cartVersion, List<CartItemResponse> items,
                               int totalQuantity, BigDecimal subtotal) { }
}
