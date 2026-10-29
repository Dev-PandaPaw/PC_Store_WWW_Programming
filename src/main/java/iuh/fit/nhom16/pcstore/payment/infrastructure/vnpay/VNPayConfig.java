package iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình VNPay Sandbox, được bind tự động từ application.yml.
 *
 * <pre>
 * vnpay:
 *   tmn-code:    YZEE4138
 *   hash-secret: OLTADMKAFDQDSFLAKWATGGNEGCIAPJQW
 *   pay-url:     https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
 *   return-url:  http://localhost:5173/checkout/complete
 *   api-url:     https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
 * </pre>
 *
 * Tất cả giá trị đều là Sandbox — không dùng cho Production.
 */
@Component
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {

    /** Mã website (Terminal ID) cấp bởi VNPay Sandbox. */
    private String tmnCode;

    /** Khóa bí mật dùng để tính checksum HMAC-SHA512. */
    private String hashSecret;

    /** URL cổng thanh toán VNPay (Sandbox). */
    private String payUrl;

    /**
     * URL VNPay sẽ redirect khách hàng về sau khi thanh toán.
     * Backend xử lý IPN riêng — đây chỉ là URL hiển thị phía client.
     */
    private String returnUrl;

    /** URL API tra cứu giao dịch VNPay (dùng cho IPN query nếu cần). */
    private String apiUrl;

    // ── Getters & setters (Spring @ConfigurationProperties yêu cầu setter) ──

    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String tmnCode) { this.tmnCode = tmnCode; }

    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String hashSecret) { this.hashSecret = hashSecret; }

    public String getPayUrl() { return payUrl; }
    public void setPayUrl(String payUrl) { this.payUrl = payUrl; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
}
