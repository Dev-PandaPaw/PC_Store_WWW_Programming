package iuh.fit.nhom16.pcstore.payment.web;

import iuh.fit.nhom16.pcstore.payment.application.PaymentService;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller xử lý thanh toán VNPay.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/v1/payments/vnpay/create-url} — frontend gọi để lấy URL redirect sang VNPay</li>
 *   <li>{@code GET  /api/v1/payments/vnpay/callback}   — VNPay redirect trình duyệt về đây sau thanh toán</li>
 * </ul>
 *
 * <p><strong>Bảo mật:</strong> Endpoint callback là public (VNPay redirect trình duyệt đến).
 * Endpoint create-url cần được bảo vệ bởi SecurityConfig (authenticated user) — hiện tại
 * fallback vào anyRequest().permitAll(); cần TV1 update SecurityConfig để thêm rule.
 */
@RestController
@RequestMapping("/api/v1/payments/vnpay")
public class PaymentController {

    private final PaymentService paymentService;
    private final VNPayConfig vnPayConfig;

    public PaymentController(PaymentService paymentService, VNPayConfig vnPayConfig) {
        this.paymentService = paymentService;
        this.vnPayConfig    = vnPayConfig;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TẠO URL THANH TOÁN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo URL thanh toán VNPay cho một đơn hàng.
     *
     * <p>Request body (JSON):
     * <pre>
     * {
     *   "orderId":        "550e8400-e29b-41d4-a716-446655440000",
     *   "amount":         1990000,
     *   "idempotencyKey": "550e8400-e29b-41d4-a716-446655440001"
     * }
     * </pre>
     *
     * <p>Response body (JSON):
     * <pre>
     * { "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=..." }
     * </pre>
     *
     * <p>{@code idempotencyKey} phải là chuỗi duy nhất do frontend tạo (UUID4 recommended)
     * và lưu lại để tránh tạo duplicate khi người dùng bấm nút 2 lần.
     *
     * @return HTTP 200 với paymentUrl, hoặc HTTP 400/409 nếu đơn không hợp lệ
     */
    @PostMapping("/create-url")
    public ResponseEntity<CreateUrlResponse> createPaymentUrl(
        @Valid @RequestBody CreateUrlRequest requestBody,
        HttpServletRequest httpRequest
    ) {
        String paymentUrl = paymentService.createVnPayPaymentUrl(
            requestBody.orderId(),
            requestBody.amount(),
            requestBody.idempotencyKey(),
            httpRequest
        );
        return ResponseEntity.ok(new CreateUrlResponse(paymentUrl));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALLBACK TỪ VNPAY (Return URL)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Nhận redirect từ VNPay sau khi khách hàng hoàn tất thanh toán (hoặc hủy).
     *
     * <p>VNPay append toàn bộ kết quả giao dịch vào query string, ví dụ:
     * {@code /api/v1/payments/vnpay/callback?vnp_TxnRef=abc&vnp_ResponseCode=00&vnp_SecureHash=...}
     *
     * <p>Backend xác minh chữ ký, cập nhật DB, rồi redirect trình duyệt về frontend.
     * Frontend đọc {@code result} và {@code txnRef} từ query string để hiển thị thông báo.
     *
     * <p><strong>Lưu ý quan trọng:</strong> Đây là Return URL (browser redirect) — KHÔNG phải IPN.
     * Không được coi URL này là bằng chứng thanh toán thành công trước khi backend xác minh chữ ký.
     *
     * @param allParams Tất cả query params do VNPay gửi (Spring tự inject từ query string)
     * @return          Redirect về frontend với result=success|failed&txnRef=...
     */
    @GetMapping("/callback")
    public RedirectView handleVnPayCallback(@RequestParam Map<String, String> allParams) {
        PaymentService.CallbackResult result = paymentService.processVnPayCallback(allParams);

        // Xây dựng URL redirect về frontend React app
        String frontendBaseUrl  = vnPayConfig.getReturnUrl(); // http://localhost:5173/checkout/complete
        String resultParam      = result.success() ? "success" : "failed";
        String txnRef           = result.txnRef() != null ? result.txnRef() : "";

        // Thêm query params để frontend hiển thị trạng thái đúng
        String redirectUrl = frontendBaseUrl
            + "?result="  + resultParam
            + "&txnRef="  + txnRef;

        // Không redirect vĩnh viễn — dùng 302 để trình duyệt follow
        RedirectView redirectView = new RedirectView(redirectUrl);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQUEST / RESPONSE RECORDS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Body của POST /create-url.
     *
     * @param orderId        UUID đơn hàng cần thanh toán
     * @param amount         Số tiền (VND, dương)
     * @param idempotencyKey Khóa duy nhất do client tạo, dùng làm vnp_TxnRef
     */
    public record CreateUrlRequest(
        @NotNull(message = "orderId không được để trống")
        UUID orderId,

        @NotNull(message = "amount không được để trống")
        @Positive(message = "amount phải là số dương")
        BigDecimal amount,

        @NotNull(message = "idempotencyKey không được để trống")
        String idempotencyKey
    ) {}

    /**
     * Body của response thành công cho POST /create-url.
     *
     * @param paymentUrl URL đầy đủ để redirect sang VNPay Sandbox
     */
    public record CreateUrlResponse(String paymentUrl) {}
}
