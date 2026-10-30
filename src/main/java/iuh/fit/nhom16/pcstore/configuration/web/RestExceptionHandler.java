package iuh.fit.nhom16.pcstore.configuration.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import iuh.fit.nhom16.pcstore.catalog.domain.CatalogConflictException;
import iuh.fit.nhom16.pcstore.catalog.domain.CatalogNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(iuh.fit.nhom16.pcstore.sales.exception.OrderException.class)
    public ProblemDetail handleOrderException(iuh.fit.nhom16.pcstore.sales.exception.OrderException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        problem.setTitle("Order request failed");
        problem.setProperty("errorCode", ex.code());
        return problem;
    }

    @ExceptionHandler({org.springframework.dao.ConcurrencyFailureException.class,
                       org.springframework.dao.DataIntegrityViolationException.class})
    public ProblemDetail handleConflict(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
        String sqlState = cause instanceof java.sql.SQLException sql ? sql.getSQLState() : "n/a";
        log.warn("Database conflict: type={}, SQLState={}", ex.getClass().getSimpleName(), sqlState);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Dữ liệu đã thay đổi. Hãy tải lại và thử lại.");
        problem.setProperty("errorCode", "DATA_CONFLICT");
        return problem;
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
                       org.springframework.http.converter.HttpMessageNotReadableException.class,
                       org.springframework.web.bind.MissingRequestHeaderException.class})
    public ProblemDetail handleMalformedRequest(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ.");
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNotFound(Exception ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên.");
    }

    @ExceptionHandler(CartException.class)
    public ProblemDetail handleCartException(CartException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(URI.create("urn:problem-type:" + ex.getErrorCode().toLowerCase().replace('_', '-')));
        problem.setTitle("Cart request failed");
        problem.setProperty("errorCode", ex.getErrorCode());
        return problem;
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    public ProblemDetail handleCatalogNotFound(CatalogNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        return problem;
    }

    @ExceptionHandler(CatalogConflictException.class)
    public ProblemDetail handleCatalogConflict(CatalogConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Catalog Conflict");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ");
        problem.setType(URI.create("urn:problem-type:validation-error"));
        problem.setTitle("Validation Failed");

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("invalidParams", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("urn:problem-type:bad-request"));
        problem.setTitle("Yêu cầu không hợp lệ");
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason() != null ? ex.getReason() : ex.getMessage());
        problem.setTitle("Yêu cầu không thành công");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi hệ thống xảy ra");
        problem.setType(URI.create("urn:problem-type:internal-error"));
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
