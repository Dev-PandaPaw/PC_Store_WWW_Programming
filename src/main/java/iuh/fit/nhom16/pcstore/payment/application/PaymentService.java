package iuh.fit.nhom16.pcstore.payment.application;

import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayConfig;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayUtil;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Xử lý nghiệp vụ thanh toán VNPay Sandbox đồng bộ với bảng {@code sales_orders},
 * {@code sales_payment_attempts}, {@code sales_payment_events} và {@code sales_history}.
 */
@Service
public class PaymentService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_REFERENCE_LENGTH = 50;

    private final VNPayConfig vnPayConfig;
    private final JdbcTemplate jdbc;

    @Autowired
    public PaymentService(VNPayConfig vnPayConfig, JdbcTemplate jdbc) {
        this.vnPayConfig = vnPayConfig;
        this.jdbc = jdbc;
    }

    private record SalesOrderRow(
            UUID id,
            String code,
            UUID customerId,
            BigDecimal total,
            String paymentMethod,
            String status,
            String paymentStatus,
            Instant expiresAt
    ) {}

    private record PaymentAttemptRow(
            UUID orderId,
            String reference,
            BigDecimal amount,
            String status,
            String paymentUrl
    ) {}

    public String createVnPayPaymentUrl(UUID orderId,
                                        BigDecimal requestedAmount,
                                        String idempotencyKey,
                                        HttpServletRequest request) {
        return createVnPayPaymentUrl(null, orderId, requestedAmount, idempotencyKey, request);
    }

    /**
     * Tạo URL chuyển hướng sang cổng thanh toán VNPay Sandbox cho đơn hàng trong {@code sales_orders}.
     */
    @Transactional
    public String createVnPayPaymentUrl(UUID actorId,
                                        UUID orderId,
                                        BigDecimal requestedAmount,
                                        String idempotencyKey,
                                        HttpServletRequest request) {
        String reference = normalizeReference(idempotencyKey);
        SalesOrderRow order = lockSalesOrder(orderId);

        if (actorId != null && !actorId.equals(order.customerId())) {
            throw OrderException.missing();
        }
        if (!"VNPAY".equalsIgnoreCase(order.paymentMethod())) {
            throw OrderException.conflict(
                    "PAYMENT_METHOD_INVALID",
                    "Đơn hàng " + order.code() + " không sử dụng phương thức thanh toán VNPay.");
        }
        if ("PAID".equalsIgnoreCase(order.paymentStatus())
                || isAlreadyProcessedStatus(order.status())) {
            throw OrderException.conflict(
                    "ORDER_ALREADY_PAID",
                    "Đơn hàng " + order.code() + " đã được thanh toán hoặc đang được xử lý.");
        }
        if ("CANCELLED".equalsIgnoreCase(order.status())
                || "EXPIRED".equalsIgnoreCase(order.status())) {
            throw OrderException.conflict(
                    "ORDER_NOT_PAYABLE",
                    "Đơn hàng " + order.code() + " đã bị hủy hoặc hết hạn thanh toán.");
        }
        if (order.expiresAt() != null && !order.expiresAt().isAfter(Instant.now())) {
            throw OrderException.conflict(
                    "ORDER_EXPIRED",
                    "Đơn hàng " + order.code() + " đã hết hạn thanh toán. Vui lòng đặt lại đơn mới.");
        }
        if (requestedAmount != null && order.total().compareTo(requestedAmount) != 0) {
            throw OrderException.conflict(
                    "AMOUNT_MISMATCH",
                    "Số tiền thanh toán (" + requestedAmount + ") không khớp với tổng tiền đơn hàng (" + order.total() + ").");
        }

        // Kiểm tra payment attempt hiện có của đơn hàng (idempotency & retry)
        PaymentAttemptRow existingAttempt = lockAttemptByOrderId(orderId);
        if (existingAttempt != null) {
            if ("SUCCESS".equalsIgnoreCase(existingAttempt.status())) {
                throw OrderException.conflict(
                        "ORDER_ALREADY_PAID",
                        "Giao dịch cho đơn hàng " + order.code() + " đã hoàn tất trước đó.");
            }
            if ("PENDING".equalsIgnoreCase(existingAttempt.status())
                    && reference.equals(existingAttempt.reference())
                    && existingAttempt.paymentUrl() != null
                    && !existingAttempt.paymentUrl().isBlank()) {
                return existingAttempt.paymentUrl();
            }
        }

        // Tránh dùng trùng mã tham chiếu cho hai đơn hàng khác nhau
        List<UUID> refOwners = jdbc.queryForList(
                "SELECT order_id FROM sales_payment_attempts WHERE reference = ?",
                UUID.class,
                reference);
        if (!refOwners.isEmpty() && !orderId.equals(refOwners.getFirst())) {
            throw OrderException.conflict(
                    "IDEMPOTENCY_CONFLICT",
                    "Mã tham chiếu thanh toán đã được sử dụng cho đơn hàng khác.");
        }

        BigDecimal authoritativeAmount = order.total().setScale(0, RoundingMode.HALF_UP);
        String vnpAmount = authoritativeAmount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
        ZonedDateTime nowVn = ZonedDateTime.now(VIETNAM_ZONE);
        String vnpCreateDate = nowVn.format(VNPAY_DATE_FORMAT);
        String vnpExpireDate = nowVn.plusMinutes(15).format(VNPAY_DATE_FORMAT);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", vnpAmount);
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", reference);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.code());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", buildBackendCallbackUrl(request));
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        String paymentUrl = vnPayConfig.getPayUrl() + "?" + VNPayUtil.buildSignedQuery(vnpParams, vnPayConfig.getHashSecret());

        jdbc.update("""
                INSERT INTO sales_payment_attempts (order_id, reference, amount, status, transaction_code, payment_url)
                VALUES (?, ?, ?, 'PENDING', NULL, ?)
                ON CONFLICT (order_id) DO UPDATE
                SET reference = EXCLUDED.reference,
                    amount = EXCLUDED.amount,
                    status = 'PENDING',
                    transaction_code = NULL,
                    payment_url = EXCLUDED.payment_url
                """,
                orderId, reference, authoritativeAmount, paymentUrl);

        return paymentUrl;
    }

    /**
     * Xử lý callback (Return URL) từ VNPay: xác minh chữ ký HMAC-SHA512, kiểm tra số tiền,
     * đảm bảo idempotency, lưu log sự kiện và cập nhật trạng thái đơn hàng trong {@code sales_orders}.
     */
    @Transactional
    public CallbackResult processVnPayCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String rawVnpAmount = params.get("vnp_Amount");

        if (receivedHash == null || txnRef == null || responseCode == null) {
            return CallbackResult.invalidRequest("Thiếu tham số bắt buộc từ VNPay");
        }

        Map<String, String> signableParams = new HashMap<>(params);
        signableParams.remove("vnp_SecureHash");
        signableParams.remove("vnp_SecureHashType");

        String expectedHash = VNPayUtil.hashAllFields(signableParams, vnPayConfig.getHashSecret());
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            return CallbackResult.invalidSignature("Chữ ký VNPay không hợp lệ");
        }

        PaymentAttemptRow attempt = lockAttemptByReference(txnRef);
        if (attempt == null) {
            return CallbackResult.failure(txnRef, null, responseCode, transactionNo, bankCode, rawVnpAmount,
                    "Không tìm thấy giao dịch tham chiếu: " + txnRef);
        }

        SalesOrderRow order = lockSalesOrderOrNull(attempt.orderId());
        if (order == null) {
            return CallbackResult.failure(txnRef, attempt.orderId(), responseCode, transactionNo, bankCode, rawVnpAmount,
                    "Không tìm thấy đơn hàng tương ứng.");
        }

        // Kiểm tra số tiền callback khớp với attempt (VNPay trả về VND * 100)
        if (!isMatchingVnpAmount(rawVnpAmount, attempt.amount())) {
            recordPaymentEvent(order.id(), transactionNo, responseCode, transactionStatus);
            return CallbackResult.failure(txnRef, order.id(), responseCode, transactionNo, bankCode, rawVnpAmount,
                    "Số tiền callback không khớp với đơn hàng.");
        }

        // Idempotency: nếu attempt đã SUCCESS thì trả về thành công luôn, không cập nhật lặp
        if ("SUCCESS".equalsIgnoreCase(attempt.status())) {
            return CallbackResult.success(txnRef, order.id(), responseCode, transactionNo, bankCode, rawVnpAmount,
                    "Giao dịch đã được ghi nhận trước đó.");
        }

        recordPaymentEvent(order.id(), transactionNo, responseCode, transactionStatus);

        boolean approved = "00".equals(responseCode)
                && (transactionStatus == null || transactionStatus.isBlank() || "00".equals(transactionStatus));

        if (!approved) {
            jdbc.update("""
                    UPDATE sales_payment_attempts
                    SET status = 'FAILED', transaction_code = ?
                    WHERE order_id = ?
                    """, transactionNo, order.id());
            if ("UNPAID".equalsIgnoreCase(order.paymentStatus())) {
                jdbc.update("""
                        UPDATE sales_orders
                        SET payment_status = 'FAILED', version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """, order.id());
            }
            return CallbackResult.failure(txnRef, order.id(), responseCode, transactionNo, bankCode, rawVnpAmount,
                    "Thanh toán không thành công (mã VNPay: " + responseCode + ")");
        }

        // Thanh toán được VNPay duyệt
        jdbc.update("""
                UPDATE sales_payment_attempts
                SET status = 'SUCCESS', transaction_code = ?
                WHERE order_id = ?
                """, transactionNo, order.id());

        if ("CANCELLED".equalsIgnoreCase(order.status()) || "EXPIRED".equalsIgnoreCase(order.status())) {
            // Đơn đã hết hạn hoặc bị hủy trước khi tiền về: không tự phục hồi đơn, đánh dấu cần đối soát/hoàn tiền
            jdbc.update("""
                    UPDATE sales_orders
                    SET payment_status = 'REVIEW_REQUIRED', version = version + 1, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, order.id());
            insertHistory(order.id(), order.status(), order.status(),
                    "VNPay báo thanh toán thành công khi đơn đã " + order.status() + " — chuyển đối soát hoàn tiền (Mã GD: " + safeText(transactionNo) + ")");
            return CallbackResult.failure(txnRef, order.id(), responseCode, transactionNo, bankCode, rawVnpAmount,
                    "Đơn hàng đã hết hạn trước khi thanh toán hoàn tất. Giao dịch đã được ghi nhận để đối soát hoàn tiền.");
        }

        jdbc.update("""
                UPDATE sales_orders
                SET status = 'CONFIRMED', payment_status = 'PAID', version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, order.id());
        insertHistory(order.id(), order.status(), "CONFIRMED",
                "Thanh toán VNPay Sandbox thành công (Mã GD: " + safeText(transactionNo) + ")");

        return CallbackResult.success(txnRef, order.id(), responseCode, transactionNo, bankCode, rawVnpAmount,
                "Thanh toán thành công");
    }

    private SalesOrderRow lockSalesOrder(UUID orderId) {
        SalesOrderRow row = lockSalesOrderOrNull(orderId);
        if (row == null) {
            throw OrderException.missing();
        }
        return row;
    }

    private SalesOrderRow lockSalesOrderOrNull(UUID orderId) {
        return jdbc.query("""
                SELECT id, code, customer_id, total, payment_method, status, payment_status, expires_at
                FROM sales_orders
                WHERE id = ?
                FOR UPDATE
                """, (rs, n) -> {
            Timestamp expiresTs = rs.getTimestamp("expires_at");
            return new SalesOrderRow(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getObject("customer_id", UUID.class),
                    rs.getBigDecimal("total"),
                    rs.getString("payment_method"),
                    rs.getString("status"),
                    rs.getString("payment_status"),
                    expiresTs != null ? expiresTs.toInstant() : null
            );
        }, orderId).stream().findFirst().orElse(null);
    }

    private PaymentAttemptRow lockAttemptByOrderId(UUID orderId) {
        return jdbc.query("""
                SELECT order_id, reference, amount, status, payment_url
                FROM sales_payment_attempts
                WHERE order_id = ?
                FOR UPDATE
                """, (rs, n) -> new PaymentAttemptRow(
                rs.getObject("order_id", UUID.class),
                rs.getString("reference"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                rs.getString("payment_url")
        ), orderId).stream().findFirst().orElse(null);
    }

    private PaymentAttemptRow lockAttemptByReference(String reference) {
        return jdbc.query("""
                SELECT order_id, reference, amount, status, payment_url
                FROM sales_payment_attempts
                WHERE reference = ?
                FOR UPDATE
                """, (rs, n) -> new PaymentAttemptRow(
                rs.getObject("order_id", UUID.class),
                rs.getString("reference"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                rs.getString("payment_url")
        ), reference).stream().findFirst().orElse(null);
    }

    private void recordPaymentEvent(UUID orderId, String transactionNo, String responseCode, String transactionStatus) {
        jdbc.update("""
                INSERT INTO sales_payment_events (order_id, transaction_code, response_code, transaction_status)
                VALUES (?, ?, ?, ?)
                """, orderId, transactionNo, responseCode, transactionStatus);
    }

    private void insertHistory(UUID orderId, String fromStatus, String toStatus, String reason) {
        jdbc.update("""
                INSERT INTO sales_history (order_id, from_status, to_status, actor_id, reason)
                VALUES (?, ?, ?, NULL, ?)
                """, orderId, fromStatus, toStatus, reason);
    }

    private static boolean isAlreadyProcessedStatus(String status) {
        return "CONFIRMED".equalsIgnoreCase(status)
                || "PROCESSING".equalsIgnoreCase(status)
                || "SHIPPING".equalsIgnoreCase(status)
                || "DELIVERED".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status);
    }

    private static String normalizeReference(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey không được để trống.");
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > MAX_REFERENCE_LENGTH) {
            throw new IllegalArgumentException("idempotencyKey không được vượt quá " + MAX_REFERENCE_LENGTH + " ký tự.");
        }
        return trimmed;
    }

    private static boolean isMatchingVnpAmount(String rawVnpAmount, BigDecimal attemptAmount) {
        if (rawVnpAmount == null || rawVnpAmount.isBlank() || attemptAmount == null) {
            return false;
        }
        try {
            BigDecimal callbackVnd = new BigDecimal(rawVnpAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
            return callbackVnd.compareTo(attemptAmount) == 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String safeText(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String buildBackendCallbackUrl(HttpServletRequest request) {
        String forwardedProto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        String forwardedHost = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        String scheme = forwardedProto != null ? forwardedProto : request.getScheme();
        if (forwardedHost != null) {
            return scheme + "://" + forwardedHost + "/api/v1/payments/vnpay/callback";
        }
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = (port == 80 && "http".equalsIgnoreCase(scheme))
                || (port == 443 && "https".equalsIgnoreCase(scheme));
        String base = isDefaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
        return base + "/api/v1/payments/vnpay/callback";
    }

    private static String firstHeaderValue(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        return header.contains(",") ? header.split(",")[0].trim() : header.trim();
    }

    public record CallbackResult(
            boolean success,
            String txnRef,
            UUID orderId,
            String responseCode,
            String transactionNo,
            String bankCode,
            String vnpAmount,
            String message,
            String errorType
    ) {
        public static CallbackResult success(String txnRef, UUID orderId, String responseCode,
                                             String transactionNo, String bankCode, String vnpAmount, String message) {
            return new CallbackResult(true, txnRef, orderId, responseCode, transactionNo, bankCode, vnpAmount, message, null);
        }

        public static CallbackResult failure(String txnRef, UUID orderId, String responseCode,
                                             String transactionNo, String bankCode, String vnpAmount, String message) {
            return new CallbackResult(false, txnRef, orderId, responseCode, transactionNo, bankCode, vnpAmount, message, "PAYMENT_FAILED");
        }

        public static CallbackResult invalidSignature(String message) {
            return new CallbackResult(false, null, null, null, null, null, null, message, "INVALID_SIGNATURE");
        }

        public static CallbackResult invalidRequest(String message) {
            return new CallbackResult(false, null, null, null, null, null, null, message, "INVALID_REQUEST");
        }
    }
}
