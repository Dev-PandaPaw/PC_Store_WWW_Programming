package iuh.fit.nhom16.pcstore.payment.service;

import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class VnpayGateway {
    private final String merchant;
    private final String secret;
    private final String gatewayUrl;
    private final String returnUrl;
    public VnpayGateway(@Value("${app.vnpay.tmn-code:}") String merchant,
                        @Value("${app.vnpay.hash-secret:}") String secret,
                        @Value("${app.vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String gatewayUrl,
                        @Value("${app.vnpay.return-url:http://localhost:5173/orders}") String returnUrl) {
        this.merchant = merchant; this.secret = secret; this.gatewayUrl = gatewayUrl; this.returnUrl = returnUrl;
    }
    public String paymentUrl(UUID order, BigDecimal amount, Instant now, Instant expiry, String ip) {
        if (merchant.isBlank() || secret.isBlank()) throw new OrderException(HttpStatus.SERVICE_UNAVAILABLE,
                "VNPAY_NOT_CONFIGURED", "Chưa cấu hình tài khoản VNPAY sandbox.");
        var format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        var params = new TreeMap<String, String>();
        params.put("vnp_Version", "2.1.0"); params.put("vnp_Command", "pay"); params.put("vnp_TmnCode", merchant);
        params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        params.put("vnp_CurrCode", "VND"); params.put("vnp_TxnRef", order.toString().replace("-", ""));
        params.put("vnp_OrderInfo", "Thanh toan PC Store " + order); params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn"); params.put("vnp_ReturnUrl", returnUrl); params.put("vnp_IpAddr", ip);
        params.put("vnp_CreateDate", format.format(now)); params.put("vnp_ExpireDate", format.format(expiry));
        return gatewayUrl + "?" + canonical(params) + "&vnp_SecureHash=" + sign(params);
    }
    public boolean verify(Map<String, String> params) {
        String supplied = params.getOrDefault("vnp_SecureHash", "");
        if (secret.isBlank() || !merchant.equals(params.get("vnp_TmnCode")) || !supplied.matches("[a-fA-F0-9]{128}")) return false;
        return MessageDigest.isEqual(HexFormat.of().parseHex(supplied), HexFormat.of().parseHex(sign(params)));
    }
    public String sign(Map<String, String> params) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(canonical(params).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) { throw new IllegalStateException("Cannot sign VNPAY request", ex); }
    }
    private String canonical(Map<String, String> params) {
        return new TreeMap<>(params).entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_") && !Set.of("vnp_SecureHash", "vnp_SecureHashType").contains(e.getKey()))
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.US_ASCII); }
}
