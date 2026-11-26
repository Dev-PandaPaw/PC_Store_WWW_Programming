package iuh.fit.nhom16.pcstore.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class CartDtos {
    private CartDtos() { }

    /**
     * Rejects decimals (e.g. 1.5, 1.0), strings ("2"), booleans, and overflows instead of coercing to int.
     */
    public static final class StrictIntegerDeserializer extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
            if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                return (Integer) ctxt.handleUnexpectedToken(
                        ctxt.constructType(Integer.class),
                        parser.currentToken(),
                        parser,
                        "Số lượng phải là số nguyên, không chấp nhận số thập phân hoặc kiểu dữ liệu khác.");
            }
            return parser.getIntValue();
        }
    }

    public static final class StrictLongDeserializer extends ValueDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext ctxt) throws JacksonException {
            if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                return (Long) ctxt.handleUnexpectedToken(
                        ctxt.constructType(Long.class),
                        parser.currentToken(),
                        parser,
                        "Phiên bản giỏ hàng phải là số nguyên.");
            }
            return parser.getLongValue();
        }
    }

    public record AddItemRequest(
            @NotNull UUID productId,
            @NotNull @Positive @Max(99) @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer quantity,
            @NotNull @Min(0) @JsonDeserialize(using = StrictLongDeserializer.class) Long cartVersion,
            UUID operationId) {

        public AddItemRequest(UUID productId, Integer quantity, Long cartVersion) {
            this(productId, quantity, cartVersion, null);
        }

        public AddItemRequest withOperationId(UUID resolvedOperationId) {
            return new AddItemRequest(productId, quantity, cartVersion, resolvedOperationId);
        }
    }

    public record UpdateItemRequest(
            @NotNull @Positive @Max(99) @JsonDeserialize(using = StrictIntegerDeserializer.class) Integer quantity,
            @NotNull @Min(0) @JsonDeserialize(using = StrictLongDeserializer.class) Long cartVersion) { }

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
