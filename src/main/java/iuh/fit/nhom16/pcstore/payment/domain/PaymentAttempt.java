package iuh.fit.nhom16.pcstore.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bản ghi một lần thử thanh toán cho đơn hàng.
 *
 * <p>Mỗi lần khách hàng bấm "Thanh toán" sẽ tạo một PaymentAttempt mới với trạng thái PENDING.
 * VNPay callback (hoặc xác nhận COD) sẽ cập nhật trạng thái về SUCCESS hoặc FAILED.
 *
 * <p>Cột DB Supabase thực tế:
 * id, order_id, payment_method, merchant_reference, amount, status,
 * transaction_id, provider_response_code, expires_at, created_at, updated_at.
 */
@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    // ── Hằng số trạng thái ───────────────────────────────────────────────────

    /** Vừa tạo, đang chờ kết quả từ cổng thanh toán. */
    public static final String STATUS_PENDING   = "PENDING";

    /** Thanh toán thành công, đã xác minh chữ ký VNPay. */
    public static final String STATUS_SUCCESS   = "SUCCESS";

    /** Cổng thanh toán từ chối hoặc timeout. */
    public static final String STATUS_FAILED    = "FAILED";

    /** Khách hàng hủy trước khi hoàn tất. */
    public static final String STATUS_CANCELLED = "CANCELLED";

    // ── Hằng số cổng thanh toán ──────────────────────────────────────────────

    public static final String GATEWAY_VNPAY = "VNPAY";
    public static final String GATEWAY_COD   = "COD";

    // ── Fields ───────────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK tới bảng orders — UUID của đơn hàng đang thanh toán. */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /**
     * Mã tham chiếu duy nhất của lần thanh toán này (merchant_reference).
     * Dùng làm {@code vnp_TxnRef} khi gửi sang VNPay,
     * và dùng để tra cứu khi VNPay callback về.
     * Có ràng buộc UNIQUE trên DB.
     */
    @Column(name = "merchant_reference", nullable = false, unique = true, length = 100)
    private String paymentReference;

    /** Tên cổng thanh toán: "VNPAY" hoặc "COD" (cột DB: payment_method). */
    @Column(name = "payment_method", nullable = false, length = 50)
    private String gateway;

    /** Số tiền thanh toán (đơn vị VND, không nhân 100). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Trạng thái hiện tại — xem hằng số STATUS_* ở trên. */
    @Column(nullable = false, length = 50)
    private String status = STATUS_PENDING;

    /**
     * Mã giao dịch do VNPay cấp (vnp_TransactionNo, cột DB: transaction_id).
     * Null cho đến khi có phản hồi thành công từ VNPay.
     */
    @Column(name = "transaction_id", length = 100)
    private String transactionCode;

    @Column(name = "provider_response_code", length = 50)
    private String providerResponseCode;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ── Lifecycle callbacks ──────────────────────────────────────────────────

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public PaymentAttempt() {}

    public PaymentAttempt(UUID orderId, String paymentReference, String gateway, BigDecimal amount) {
        this.orderId          = orderId;
        this.paymentReference = paymentReference;
        this.gateway          = gateway;
        this.amount           = amount;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public UUID getId()                                             { return id; }
    public UUID getOrderId()                                        { return orderId; }
    public String getPaymentReference()                             { return paymentReference; }
    public String getGateway()                                      { return gateway; }
    public BigDecimal getAmount()                                   { return amount; }
    public String getStatus()                                       { return status; }
    public String getTransactionCode()                              { return transactionCode; }
    public String getProviderResponseCode()                         { return providerResponseCode; }
    public OffsetDateTime getExpiresAt()                            { return expiresAt; }
    public OffsetDateTime getCreatedAt()                            { return createdAt; }
    public OffsetDateTime getUpdatedAt()                            { return updatedAt; }

    public void setStatus(String status)                            { this.status = status; }
    public void setTransactionCode(String transactionCode)          { this.transactionCode = transactionCode; }
    public void setProviderResponseCode(String responseCode)        { this.providerResponseCode = responseCode; }
    public void setExpiresAt(OffsetDateTime expiresAt)              { this.expiresAt = expiresAt; }
}
