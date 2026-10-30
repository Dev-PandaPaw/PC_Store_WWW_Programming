package iuh.fit.nhom16.pcstore.cart.exception;

import org.springframework.http.HttpStatus;

public class CartException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public CartException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
