package iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Tiện ích xử lý tích hợp VNPay Sandbox v2.1.0.
 */
public final class VNPayUtil {

    private VNPayUtil() {
    }

    /**
     * Tính HMAC-SHA512 của {@code data} với khóa {@code key}, trả về chuỗi hex chữ thường.
     */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexResult = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hexResult.append(String.format("%02x", b));
            }
            return hexResult.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tính HMAC-SHA512: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy địa chỉ IPv4 thực của client (chuẩn hóa IPv6 loopback về 127.0.0.1 theo yêu cầu VNPay).
     */
    public static String getIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_CLIENT_IP"
        };

        String resolved = null;
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                resolved = ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
                break;
            }
        }
        if (resolved == null || resolved.isBlank()) {
            resolved = request.getRemoteAddr();
        }
        if (resolved == null || resolved.isBlank()
                || "0:0:0:0:0:0:0:1".equals(resolved) || "::1".equals(resolved)) {
            return "127.0.0.1";
        }
        return resolved;
    }

    /**
     * Tạo chuỗi dữ liệu ký theo VNPay v2.1.0: sắp xếp theo key và URL-encode giá trị trước khi ghép {@code key=value}.
     */
    public static String buildHashData(Map<String, String> fields) {
        return new TreeMap<>(fields).entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * Tạo checksum VNPay HMAC-SHA512 từ toàn bộ tham số (đã loại {@code vnp_SecureHash}).
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        return hmacSHA512(secretKey, buildHashData(fields));
    }

    /**
     * Build query string đã URL-encode kèm {@code vnp_SecureHash} ở cuối theo VNPay v2.1.0.
     */
    public static String buildSignedQuery(Map<String, String> fields, String secretKey) {
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : new TreeMap<>(fields).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                continue;
            }
            if (!first) {
                hashData.append('&');
                query.append('&');
            }
            String encodedValue = urlEncode(value);
            hashData.append(key).append('=').append(encodedValue);
            query.append(urlEncode(key)).append('=').append(encodedValue);
            first = false;
        }
        String secureHash = hmacSHA512(secretKey, hashData.toString());
        return query + "&vnp_SecureHash=" + secureHash;
    }

    public static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
