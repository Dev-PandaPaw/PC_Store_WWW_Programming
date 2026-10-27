package iuh.fit.nhom16.pcstore.sales.dto;

import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() { }
    public enum PaymentMethod { COD, VNPAY }
    public record Shipping(@NotBlank @Size(min=2,max=100) String recipient,
                           @NotBlank @Pattern(regexp="(?:0[35789][0-9]{8}|\\+84[35789][0-9]{8})") String phone,
                           @NotBlank @Size(max=100) String province,
                           @NotBlank @Size(max=100) String ward,
                           @NotBlank @Size(min=5,max=255) String addressLine) {
        public String address() { return addressLine.trim() + ", " + ward.trim() + ", " + province.trim(); }
        public String normalizedPhone() { return phone.startsWith("0") ? "+84" + phone.substring(1) : phone; }
    }
    public record QuoteRequest(@PositiveOrZero long cartVersion, @NotNull @Valid Shipping shipping) { }
    public record PlaceRequest(@NotNull UUID quoteId, @NotNull PaymentMethod paymentMethod) { }
    public record TransitionRequest(@PositiveOrZero long version, @NotNull OrderStatus status,
                                    @NotBlank @Size(max=500) String reason) { }
    public record QuantityRequest(@PositiveOrZero long version, @Min(1) @Max(99) int quantity,
                                  @NotBlank @Size(max=500) String reason) { }
    public record Line(UUID productId, String name, String sku, BigDecimal unitPrice, int quantity, int warrantyMonths) {
        public BigDecimal subtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    }
    public record Quote(UUID id, UUID customerId, long cartVersion, String recipient, String phone,
                        String address, Instant expiresAt, List<Line> items, BigDecimal shippingFee) {
        @com.fasterxml.jackson.annotation.JsonProperty
        public BigDecimal subtotal() { return items.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add); }
        @com.fasterxml.jackson.annotation.JsonProperty
        public BigDecimal total() { return subtotal().add(shippingFee); }
    }
    public record Order(UUID id, String code, UUID customerId, String email, String recipient, String phone,
                        String address, PaymentMethod paymentMethod, OrderStatus status, String paymentStatus,
                        String reservationStatus, long version, Instant expiresAt, Instant createdAt,
                        BigDecimal shippingFee, BigDecimal total, String paymentReference, String paymentUrl,
                        List<Line> items, List<History> history) { }
    public record History(String fromStatus, String toStatus, UUID actorId, String reason, Instant createdAt) { }
    public record Account(UUID id, String name, String email, String phone, boolean phoneVerified,
                          List<String> roles) { }
}
