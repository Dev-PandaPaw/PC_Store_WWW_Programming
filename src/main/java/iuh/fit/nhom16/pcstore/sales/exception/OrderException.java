package iuh.fit.nhom16.pcstore.sales.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class OrderException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> details;

    public OrderException(HttpStatus status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public OrderException(HttpStatus status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }

    public static OrderException badRequest(String code, String message) {
        return new OrderException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }

    public static OrderException conflict(String code, String message, Map<String, Object> details) {
        return new OrderException(HttpStatus.CONFLICT, code, message, details);
    }

    public static OrderException missing() {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy dữ liệu.");
    }
}
