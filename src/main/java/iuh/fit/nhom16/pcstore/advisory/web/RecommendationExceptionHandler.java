package iuh.fit.nhom16.pcstore.advisory.web;

import iuh.fit.nhom16.pcstore.advisory.application.RecommendationFailure;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Map;

@Order(-10)
@RestControllerAdvice(assignableTypes=RecommendationController.class)
public class RecommendationExceptionHandler {
    @ExceptionHandler(RecommendationFailure.class)
    public ResponseEntity<?> failure(RecommendationFailure error) {
        return ResponseEntity.status(error.status).cacheControl(CacheControl.noStore())
                .body(Map.of("error",Map.of("code",error.code,"message",error.getMessage())));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> invalidId() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                .body(Map.of("error",Map.of("code","INVALID_ID","message","ID sản phẩm phải là UUID hợp lệ.")));
    }
}
