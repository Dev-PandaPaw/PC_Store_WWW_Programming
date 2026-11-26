package iuh.fit.nhom16.pcstore.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() { }

    public enum PaymentMethod { COD, VNPAY }

    public enum ShippingMethod {
        EXPRESS,      // Vận chuyển nhanh toàn quốc
        INSTANT,      // Ship hỏa tốc 2h (nội thành TP.HCM & Hà Nội)
        STORE_PICKUP  // Nhận tại cửa hàng
    }

    public record Shipping(
            @NotBlank @Size(min = 2, max = 100) String recipient,
            @NotBlank @Pattern(regexp = "(?:0[35789][0-9]{8}|\\+84[35789][0-9]{8})") String phone,
            @NotBlank @Size(max = 100) String province,
            @NotBlank @Size(max = 100) String ward,
            @NotBlank @Size(min = 5, max = 255) String addressLine,
            @NotBlank @Size(max = 100) String district) {

        public Shipping(String recipient, String phone, String province, String ward, String addressLine) {
            this(recipient, phone, province, ward, addressLine, null);
        }

        public String address() {
            return addressLine.trim() + ", " + ward.trim()
                    + (district == null || district.isBlank() ? "" : ", " + district.trim()) + ", " + province.trim();
        }

        public String normalizedPhone() {
            return phone.startsWith("0") ? "+84" + phone.substring(1) : phone;
        }
    }

    public record QuoteRequest(
            @PositiveOrZero long cartVersion,
            @NotNull @Valid Shipping shipping,
            List<UUID> selectedProductIds,
            ShippingMethod shippingMethod,
            @Size(max = 50) String voucherCode,
            PaymentMethod paymentMethod) {

        public QuoteRequest(long cartVersion, Shipping shipping) {
            this(cartVersion, shipping, null, ShippingMethod.EXPRESS, null, PaymentMethod.COD);
        }

        public ShippingMethod resolvedShippingMethod() {
            return shippingMethod == null ? ShippingMethod.EXPRESS : shippingMethod;
        }

        public PaymentMethod resolvedPaymentMethod() {
            return paymentMethod == null ? PaymentMethod.COD : paymentMethod;
        }

        public String normalizedVoucherCode() {
            if (voucherCode == null || voucherCode.isBlank()) return null;
            return voucherCode.trim().toUpperCase();
        }
    }

    public record PlaceRequest(@NotNull UUID quoteId, @NotNull PaymentMethod paymentMethod) { }

    public record TransitionRequest(
            @PositiveOrZero long version,
            @NotNull OrderStatus status,
            @NotBlank @Size(max = 500) String reason) { }

    public record QuantityRequest(
            @PositiveOrZero long version,
            @Min(1) @Max(99) int quantity,
            @NotBlank @Size(max = 500) String reason) { }

    public record Line(
            UUID productId,
            String name,
            String sku,
            BigDecimal unitPrice,
            int quantity,
            int warrantyMonths) {

        @JsonProperty
        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(0, RoundingMode.HALF_UP);
        }
    }

    public record VoucherRuleSnapshot(
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minOrderSubtotal,
            BigDecimal maxDiscountAmount) {

        public boolean isEligible(BigDecimal subtotal) {
            if (code == null || discountType == null || discountValue == null) return false;
            BigDecimal min = minOrderSubtotal == null ? BigDecimal.ZERO : minOrderSubtotal;
            return subtotal != null && subtotal.compareTo(min) >= 0;
        }

        public BigDecimal calculateDiscount(BigDecimal subtotal) {
            if (!isEligible(subtotal)) {
                return BigDecimal.ZERO;
            }
            BigDecimal raw;
            if ("PERCENT".equalsIgnoreCase(discountType)) {
                raw = subtotal.multiply(discountValue)
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (maxDiscountAmount != null && raw.compareTo(maxDiscountAmount) > 0) {
                    raw = maxDiscountAmount.setScale(0, RoundingMode.HALF_UP);
                }
            } else {
                raw = discountValue.setScale(0, RoundingMode.HALF_UP);
            }
            // Merchandise voucher discount never exceeds merchandise subtotal and is never negative
            return raw.min(subtotal).max(BigDecimal.ZERO);
        }
    }

    public record PricingBreakdown(
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal taxAmount,
            String voucherCode,
            BigDecimal discountAmount,
            BigDecimal total) {

        public static PricingBreakdown of(BigDecimal subtotal, BigDecimal shippingFee,
                                          BigDecimal taxAmount, String voucherCode, BigDecimal discountAmount) {
            BigDecimal sub = normalizeMoney(subtotal);
            BigDecimal ship = normalizeMoney(shippingFee);
            BigDecimal tax = normalizeMoney(taxAmount);
            BigDecimal cappedDiscount = normalizeMoney(discountAmount).min(sub).max(BigDecimal.ZERO);
            String effectiveCode = (voucherCode == null || voucherCode.isBlank() || cappedDiscount.signum() == 0)
                    ? null
                    : voucherCode;
            BigDecimal grand = sub.add(ship).add(tax).subtract(cappedDiscount).max(BigDecimal.ZERO);
            return new PricingBreakdown(sub, ship, tax, effectiveCode, cappedDiscount, grand);
        }

        private static BigDecimal normalizeMoney(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
        }
    }

    public record Quote(
            UUID id,
            UUID customerId,
            long cartVersion,
            String recipient,
            String phone,
            String address,
            Integer provinceCode,
            Integer districtCode,
            Integer wardCode,
            ShippingMethod shippingMethod,
            PaymentMethod paymentMethod,
            Instant expiresAt,
            List<Line> items,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal taxAmount,
            String voucherCode,
            VoucherRuleSnapshot voucherRule,
            BigDecimal discountAmount,
            BigDecimal total) {

        public Quote(UUID id, UUID customerId, long cartVersion, String recipient, String phone,
                     String address, Instant expiresAt, List<Line> items, BigDecimal shippingFee) {
            this(
                    id,
                    customerId,
                    cartVersion,
                    recipient,
                    phone,
                    address,
                    79,
                    760,
                    26734,
                    ShippingMethod.EXPRESS,
                    PaymentMethod.COD,
                    expiresAt,
                    items,
                    items.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                    shippingFee,
                    BigDecimal.ZERO,
                    null,
                    null,
                    BigDecimal.ZERO,
                    items.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add).add(shippingFee));
        }
    }

    public record Order(
            UUID id,
            String code,
            UUID customerId,
            String email,
            String recipient,
            String phone,
            String address,
            Integer provinceCode,
            Integer districtCode,
            Integer wardCode,
            ShippingMethod shippingMethod,
            PaymentMethod paymentMethod,
            OrderStatus status,
            String paymentStatus,
            String reservationStatus,
            long version,
            Instant expiresAt,
            Instant createdAt,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal taxAmount,
            String voucherCode,
            VoucherRuleSnapshot voucherRule,
            BigDecimal discountAmount,
            BigDecimal total,
            List<Line> items,
            List<History> history) {

        public Order(UUID id, String code, UUID customerId, String email, String recipient, String phone,
                     String address, PaymentMethod paymentMethod, OrderStatus status, String paymentStatus,
                     String reservationStatus, long version, Instant expiresAt, Instant createdAt,
                     BigDecimal shippingFee, BigDecimal total, List<Line> items, List<History> history) {
            this(
                    id,
                    code,
                    customerId,
                    email,
                    recipient,
                    phone,
                    address,
                    79,
                    760,
                    26734,
                    ShippingMethod.EXPRESS,
                    paymentMethod,
                    status,
                    paymentStatus,
                    reservationStatus,
                    version,
                    expiresAt,
                    createdAt,
                    items.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                    shippingFee,
                    BigDecimal.ZERO,
                    null,
                    null,
                    BigDecimal.ZERO,
                    total,
                    items,
                    history);
        }
    }

    public record History(String fromStatus, String toStatus, UUID actorId, String reason, Instant createdAt) { }

    public record Account(UUID id, String name, String email, String phone, boolean phoneVerified, List<String> roles) { }

    public record VoucherView(
            String code,
            String description,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minOrderSubtotal,
            BigDecimal maxDiscountAmount,
            Instant expiresAt) { }

    public record ShippingOptionView(
            ShippingMethod method,
            String label,
            String description,
            BigDecimal fee,
            boolean available,
            String unavailableReason) { }
}
