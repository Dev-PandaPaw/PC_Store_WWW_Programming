package iuh.fit.nhom16.pcstore.sales.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Order (bản thu gọn) khớp với cấu trúc bảng orders trên database thực tế (Supabase).
 *
 * <p>Danh sách cột trên Supabase:
 * id, order_code, user_id, status, reservation_status,
 * recipient_name_snapshot, contact_phone_snapshot, shipping_address_snapshot,
 * subtotal, shipping_fee, grand_total, payment_method,
 * expires_at, delivered_at, version, created_at, updated_at.
 */
@Entity
@Table(name = "orders")
public class Order {

    // ── Hằng số trạng thái đơn hàng (status) ─────────────────────────────────
    public static final String STATUS_AWAITING_PAYMENT      = "AWAITING_PAYMENT";
    public static final String STATUS_AWAITING_CONFIRMATION = "AWAITING_CONFIRMATION";
    public static final String STATUS_CONFIRMED             = "CONFIRMED";
    public static final String STATUS_SHIPPING              = "SHIPPING";
    public static final String STATUS_DELIVERED             = "DELIVERED";
    public static final String STATUS_CANCELLED             = "CANCELLED";
    public static final String STATUS_EXPIRED               = "EXPIRED";
    public static final String STATUS_PENDING_PAYMENT       = "PENDING_PAYMENT";

    // ── Hằng số phương thức thanh toán (payment_method) ──────────────────────
    public static final String PAYMENT_METHOD_COD   = "COD";
    public static final String PAYMENT_METHOD_VNPAY = "VNPAY";

    // ── Hằng số trạng thái giữ tồn (reservation_status) ──────────────────────
    public static final String RESERVATION_HELD     = "HELD";
    public static final String RESERVATION_RELEASED = "RELEASED";
    public static final String RESERVATION_CONSUMED = "CONSUMED";

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_code")
    private String orderCode;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "reservation_status")
    private String reservationStatus;

    @Column(name = "recipient_name_snapshot")
    private String recipientNameSnapshot;

    @Column(name = "contact_phone_snapshot")
    private String contactPhoneSnapshot;

    @Column(name = "shipping_address_snapshot")
    private String shippingAddressSnapshot;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @Column(name = "shipping_fee")
    private BigDecimal shippingFee;

    /**
     * Tổng tiền đơn hàng khách phải trả.
     * Tên cột DB: grand_total.
     */
    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public Order() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public UUID getId()                                         { return id; }
    public void setId(UUID id)                                  { this.id = id; }

    public String getOrderCode()                                { return orderCode; }
    public void setOrderCode(String orderCode)                  { this.orderCode = orderCode; }

    public UUID getUserId()                                     { return userId; }
    public void setUserId(UUID userId)                          { this.userId = userId; }

    public String getStatus()                                   { return status; }
    public void setStatus(String status)                        { this.status = status; }

    public String getReservationStatus()                        { return reservationStatus; }
    public void setReservationStatus(String reservationStatus)  { this.reservationStatus = reservationStatus; }

    public String getRecipientNameSnapshot()                    { return recipientNameSnapshot; }
    public void setRecipientNameSnapshot(String name)           { this.recipientNameSnapshot = name; }

    public String getContactPhoneSnapshot()                     { return contactPhoneSnapshot; }
    public void setContactPhoneSnapshot(String phone)           { this.contactPhoneSnapshot = phone; }

    public String getShippingAddressSnapshot()                  { return shippingAddressSnapshot; }
    public void setShippingAddressSnapshot(String address)      { this.shippingAddressSnapshot = address; }

    public BigDecimal getSubtotal()                             { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)                { this.subtotal = subtotal; }

    public BigDecimal getShippingFee()                          { return shippingFee; }
    public void setShippingFee(BigDecimal shippingFee)          { this.shippingFee = shippingFee; }

    public BigDecimal getGrandTotal()                           { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal)            { this.grandTotal = grandTotal; }

    public String getPaymentMethod()                            { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod)          { this.paymentMethod = paymentMethod; }

    public OffsetDateTime getExpiresAt()                        { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt)          { this.expiresAt = expiresAt; }

    public OffsetDateTime getDeliveredAt()                      { return deliveredAt; }
    public void setDeliveredAt(OffsetDateTime deliveredAt)      { this.deliveredAt = deliveredAt; }

    public Long getVersion()                                    { return version; }
    public void setVersion(Long version)                        { this.version = version; }

    public OffsetDateTime getCreatedAt()                        { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt)          { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt()                        { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt)          { this.updatedAt = updatedAt; }
}
