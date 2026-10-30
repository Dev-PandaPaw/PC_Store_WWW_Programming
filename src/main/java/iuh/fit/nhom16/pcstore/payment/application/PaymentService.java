package iuh.fit.nhom16.pcstore.payment.application;

import iuh.fit.nhom16.pcstore.payment.domain.PaymentAttempt;
import iuh.fit.nhom16.pcstore.payment.infrastructure.PaymentAttemptRepository;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayConfig;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayUtil;
import iuh.fit.nhom16.pcstore.sales.domain.Order;
import iuh.fit.nhom16.pcstore.sales.infrastructure.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Xử lý nghiệp vụ thanh toán: tạo URL VNPay và xử lý callback.
 */
@Service
public class PaymentService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VNPayConfig vnPayConfig;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;

    public PaymentService(VNPayConfig vnPayConfig,
                          PaymentAttemptRepository paymentAttemptRepository,
                          OrderRepository orderRepository) {
        this.vnPayConfig               = vnPayConfig;
        this.paymentAttemptRepository  = paymentAttemptRepository;
        this.orderRepository           = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TẠO URL THANH TOÁN VNPAY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo URL chuyển hướng sang cổng thanh toán VNPay Sandbox.
     *
     * <p>Luồng:
     * <ol>
     *   <li>Kiểm tra đơn hàng hợp lệ và chưa được thanh toán.</li>
     *   <li>Kiểm tra idempotency: nếu đã có PaymentAttempt PENDING với cùng {@code idempotencyKey},
     *       tái sử dụng bản ghi đó thay vì tạo mới (tránh duplicate).</li>
     *   <li>Tạo PaymentAttempt mới nếu chưa có.</li>
     *   <li>Build tham số VNPay v2.1.0 và tính vnp_SecureHash.</li>
     *   <li>Trả về URL đầy đủ (encode) để frontend redirect.</li>
     * </ol>
     *
     * @param orderId         UUID đơn hàng cần thanh toán
     * @param amount          Số tiền (VND, không nhân 100) — phải khớp với grand_total của đơn
     * @param idempotencyKey  Khóa duy nhất của lần yêu cầu này, dùng làm vnp_TxnRef.
     *                        Thường là UUID.randomUUID().toString() do frontend tạo và lưu lại.
     * @param request         HTTP request hiện tại (để lấy IP và base URL)
     * @return                URL đầy đủ để redirect sang VNPay Sandbox
     */
    @Transactional
    public String createVnPayPaymentUrl(UUID orderId,
                                        BigDecimal amount,
                                        String idempotencyKey,
                                        HttpServletRequest request) {

        // 1. Kiểm tra đơn hàng tồn tại và chưa thanh toán
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Không tìm thấy đơn hàng với id: " + orderId));

        if (Order.STATUS_CONFIRMED.equals(order.getStatus())
                || Order.STATUS_AWAITING_CONFIRMATION.equals(order.getStatus())
                || Order.STATUS_SHIPPING.equals(order.getStatus())
                || Order.STATUS_DELIVERED.equals(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng " + orderId + " đã được thanh toán hoặc đang được xử lý.");
        }

        if (Order.STATUS_CANCELLED.equals(order.getStatus())
                || Order.STATUS_EXPIRED.equals(order.getStatus())) {
            throw new IllegalStateException("Đơn hàng " + orderId + " đã bị hủy hoặc hết hạn.");
        }

        if (order.getGrandTotal() != null && amount.compareTo(order.getGrandTotal()) != 0) {
            throw new IllegalArgumentException(
                "Số tiền thanh toán (" + amount + ") không khớp với tổng tiền đơn hàng (" + order.getGrandTotal() + ")");
        }

        // 2. Idempotency: tái sử dụng PENDING attempt nếu đã tạo trước đó với cùng key
        paymentAttemptRepository
            .findByPaymentReferenceForUpdate(idempotencyKey)
            .ifPresent(existing -> {
                if (PaymentAttempt.STATUS_SUCCESS.equals(existing.getStatus())) {
                    throw new IllegalStateException("Giao dịch " + idempotencyKey + " đã được xử lý thành công.");
                }
                // PENDING hoặc FAILED: bỏ qua, cho phép tiếp tục tạo URL mới
            });

        // 3. Tạo mới PaymentAttempt chỉ khi chưa tồn tại
        boolean attemptAlreadyExists = paymentAttemptRepository
            .findByPaymentReferenceForUpdate(idempotencyKey)
            .isPresent();

        if (!attemptAlreadyExists) {
            PaymentAttempt attempt = new PaymentAttempt(
                orderId, idempotencyKey, PaymentAttempt.GATEWAY_VNPAY, amount
            );
            paymentAttemptRepository.save(attempt);
        }

        // 4. Build tham số VNPay
        String vnpCreateDate = ZonedDateTime.now(VIETNAM_ZONE).format(VNPAY_DATE_FORMAT);
        String vnpExpireDate = ZonedDateTime.now(VIETNAM_ZONE).plusMinutes(15).format(VNPAY_DATE_FORMAT);
        String vnpIpAddr     = VNPayUtil.getIpAddress(request);
        String backendCallbackUrl = buildBackendCallbackUrl(request);

        // vnp_Amount phải là số nguyên (VND × 100), không có dấu thập phân
        String vnpAmount = amount.multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version",    "2.1.0");
        vnpParams.put("vnp_Command",    "pay");
        vnpParams.put("vnp_TmnCode",    vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount",     vnpAmount);
        vnpParams.put("vnp_CurrCode",   "VND");
        vnpParams.put("vnp_TxnRef",     idempotencyKey);
        vnpParams.put("vnp_OrderInfo",  "Thanh toan don hang " + orderId);
        vnpParams.put("vnp_OrderType",  "other");
        vnpParams.put("vnp_Locale",     "vn");
        vnpParams.put("vnp_ReturnUrl",  backendCallbackUrl);
        vnpParams.put("vnp_IpAddr",     vnpIpAddr);
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // 5. Tính chữ ký — dùng raw values (chưa URL-encode) đúng theo VNPay spec
        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnPayConfig.getHashSecret());

        // 6. Build URL — values phải được URL-encode khi ghép vào query string
        StringBuilder urlBuilder = new StringBuilder(vnPayConfig.getPayUrl()).append("?");
        new TreeMap<>(vnpParams).forEach((key, value) ->
            urlBuilder
                .append(urlEncode(key))
                .append("=")
                .append(urlEncode(value))
                .append("&")
        );
        // Append vnp_SecureHash ở cuối (không nằm trong phần hash)
        urlBuilder.append("vnp_SecureHash=").append(secureHash);

        return urlBuilder.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XỬ LÝ CALLBACK VNPAY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xử lý phản hồi VNPay gửi về (Return URL redirect).
     *
     * <p>Bảo đảm:
     * <ul>
     *   <li><strong>Xác minh chữ ký:</strong> tính lại HMAC-SHA512 và so sánh với vnp_SecureHash.</li>
     *   <li><strong>Idempotency:</strong> nếu PaymentAttempt đã SUCCESS, trả về ngay mà không cập nhật lặp.</li>
     *   <li><strong>Atomic:</strong> cập nhật PaymentAttempt và Order trong cùng một transaction.</li>
     * </ul>
     *
     * @param params Tất cả tham số VNPay gửi về (gồm vnp_SecureHash)
     * @return       Kết quả xử lý — controller dùng để redirect về frontend
     */
    @Transactional
    public CallbackResult processVnPayCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        String txnRef       = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        if (receivedHash == null || txnRef == null || responseCode == null) {
            return CallbackResult.invalidRequest("Thiếu tham số bắt buộc từ VNPay");
        }

        // 1. Tách vnp_SecureHash và vnp_SecureHashType ra trước khi tính lại hash
        Map<String, String> signableParams = new HashMap<>(params);
        signableParams.remove("vnp_SecureHash");
        signableParams.remove("vnp_SecureHashType");

        // 2. Xác minh chữ ký
        String expectedHash = VNPayUtil.hashAllFields(signableParams, vnPayConfig.getHashSecret());
        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            return CallbackResult.invalidSignature("Chữ ký VNPay không hợp lệ — có thể bị giả mạo");
        }

        // 3. Tra cứu PaymentAttempt với lock để tránh xử lý đồng thời
        PaymentAttempt attempt = paymentAttemptRepository
            .findByPaymentReferenceForUpdate(txnRef)
            .orElseGet(() -> {
                // Trường hợp hiếm: callback đến trước khi attempt được lưu xong
                // Không throw để tránh lỗi 500 — VNPay có thể retry
                return null;
            });

        if (attempt == null) {
            return CallbackResult.failure(txnRef, "Không tìm thấy bản ghi giao dịch: " + txnRef);
        }

        // 4. Idempotency: đã xử lý thành công rồi → không làm gì thêm
        if (PaymentAttempt.STATUS_SUCCESS.equals(attempt.getStatus())) {
            return CallbackResult.success(txnRef, "Giao dịch đã được xử lý trước đó (idempotent)");
        }

        // 5. VNPay từ chối giao dịch (responseCode != "00")
        if (!"00".equals(responseCode)) {
            attempt.setStatus(PaymentAttempt.STATUS_FAILED);
            attempt.setTransactionCode(transactionNo);
            paymentAttemptRepository.save(attempt);
            return CallbackResult.failure(txnRef, "VNPay từ chối giao dịch — mã lỗi: " + responseCode);
        }

        // 6. Thanh toán thành công — cập nhật PaymentAttempt
        attempt.setStatus(PaymentAttempt.STATUS_SUCCESS);
        attempt.setTransactionCode(transactionNo);
        paymentAttemptRepository.save(attempt);

        // 7. Cập nhật trạng thái đơn hàng
        orderRepository.findById(attempt.getOrderId()).ifPresent(order -> {
            order.setStatus(Order.STATUS_CONFIRMED);
            orderRepository.save(order);
        });

        return CallbackResult.success(txnRef, "Thanh toán thành công");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Xây dựng URL backend callback từ request hiện tại.
     * VNPay sẽ redirect trình duyệt khách hàng về URL này sau khi thanh toán.
     */
    private String buildBackendCallbackUrl(HttpServletRequest request) {
        String scheme = request.getScheme();           // "http" hoặc "https"
        String host   = request.getServerName();       // "localhost"
        int    port   = request.getServerPort();       // 8080

        // Bỏ qua port mặc định khi build URL
        boolean isDefaultPort = (port == 80 && "http".equals(scheme))
                             || (port == 443 && "https".equals(scheme));

        String base = isDefaultPort
            ? scheme + "://" + host
            : scheme + "://" + host + ":" + port;

        return base + "/api/v1/payments/vnpay/callback";
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INNER RECORD — Kết quả callback
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kết quả xử lý callback VNPay.
     * Controller dùng {@code success} để quyết định redirect về trang thành công hay thất bại.
     */
    public record CallbackResult(boolean success, String txnRef, String message, String errorType) {

        public static CallbackResult success(String txnRef, String message) {
            return new CallbackResult(true, txnRef, message, null);
        }

        public static CallbackResult failure(String txnRef, String message) {
            return new CallbackResult(false, txnRef, message, "PAYMENT_FAILED");
        }

        public static CallbackResult invalidSignature(String message) {
            return new CallbackResult(false, null, message, "INVALID_SIGNATURE");
        }

        public static CallbackResult invalidRequest(String message) {
            return new CallbackResult(false, null, message, "INVALID_REQUEST");
        }
    }
}
