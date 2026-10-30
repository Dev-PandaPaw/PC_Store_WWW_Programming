package iuh.fit.nhom16.pcstore.sales.exception;

import org.springframework.http.HttpStatus;

public class OrderException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public OrderException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public static OrderException conflict(String code, String message) {
        return new OrderException(HttpStatus.CONFLICT, code, message);
    }
    public static OrderException missing() {
        return new OrderException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy dữ liệu.");
    }
}
