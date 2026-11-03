package iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Tiện ích xử lý tích hợp VNPay Sandbox.
 *
 * <p>Lớp này là pure-static utility, không có state và không phụ thuộc Spring context.
 * Dễ unit-test trực tiếp mà không cần context.
 *
 * <p>Tham khảo: VNPay Technical Specification v2.1.0 — mục "Tạo chữ ký checksum".
 */
public final class VNPayUtil {

    private VNPayUtil() {
        // Không cho phép khởi tạo — đây là utility class
    }

    /**
     * Tính HMAC-SHA512 của {@code data} với khóa {@code key}.
     *
     * <p>Kết quả trả về dạng hex string chữ thường (lowercase), đúng định dạng VNPay yêu cầu.
     *
     * @param key       khóa bí mật (hashSecret từ VNPayConfig)
     * @param data      chuỗi cần băm (query string chưa encode)
     * @return          chuỗi hex 128 ký tự
     * @throws IllegalStateException nếu JVM không hỗ trợ HmacSHA512 (không bao giờ xảy ra trên JDK 11+)
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
     * Lấy địa chỉ IP thực của client, có xử lý trường hợp request qua proxy/load-balancer.
     *
     * <p>Ưu tiên: X-Forwarded-For → Proxy-Client-IP → WL-Proxy-Client-IP → RemoteAddr.
     * Nếu X-Forwarded-For chứa nhiều IP (danh sách qua nhiều proxy), lấy IP đầu tiên (IP gốc).
     *
     * @param request HTTP request hiện tại
     * @return IP string (ví dụ: "127.0.0.1") — không bao giờ null
     */
    public static String getIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể là "client, proxy1, proxy2" — lấy IP client đầu tiên
                return ip.contains(",") ? ip.split(",")[0].trim() : ip.trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Tạo checksum VNPay từ toàn bộ tham số thanh toán.
     *
     * <p>Thuật toán theo VNPay Technical Spec v2.1.0:
     * <ol>
     *   <li>Loại bỏ các entry có value null hoặc blank.</li>
     *   <li>Sắp xếp các key theo thứ tự từ điển (lexicographic, case-sensitive).</li>
     *   <li>Nối thành chuỗi {@code key=value&key=value} — <strong>giá trị RAW, không URL-encode</strong>.</li>
     *   <li>Tính HMAC-SHA512 với {@code secretKey}.</li>
     * </ol>
     *
     * <p><strong>Lưu ý:</strong> Khi gọi hàm này để xác minh callback, phải loại bỏ
     * {@code vnp_SecureHash} và {@code vnp_SecureHashType} ra khỏi {@code fields} trước.
     *
     * @param fields    toàn bộ tham số (trừ vnp_SecureHash)
     * @param secretKey khóa bí mật từ VNPayConfig.getHashSecret()
     * @return          checksum hex 128 ký tự
     */
    public static String hashAllFields(Map<String, String> fields, String secretKey) {
        // TreeMap tự sắp xếp key theo thứ tự từ điển — đúng yêu cầu VNPay
        String rawQueryString = new TreeMap<>(fields).entrySet().stream()
            .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining("&"));

        return hmacSHA512(secretKey, rawQueryString);
    }
}
