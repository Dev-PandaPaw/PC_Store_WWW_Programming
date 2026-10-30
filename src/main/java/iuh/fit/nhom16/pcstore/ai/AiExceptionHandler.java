package iuh.fit.nhom16.pcstore.ai;

import iuh.fit.nhom16.pcstore.advisory.AiAdvisorController;
import iuh.fit.nhom16.pcstore.catalog.SmartSearchController;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(-10)
@RestControllerAdvice(assignableTypes = {AiAdvisorController.class, SmartSearchController.class})
public class AiExceptionHandler {
    @ExceptionHandler(AiFailure.class)
    public ResponseEntity<?> handle(AiFailure failure) {
        Map<String,Object> error = new LinkedHashMap<>();
        error.put("code",failure.code); error.put("message",failure.getMessage());
        var response = ResponseEntity.status(failure.status).cacheControl(CacheControl.noStore());
        if (failure.retryAfterSeconds > 0) {
            error.put("retryAfterSeconds",failure.retryAfterSeconds);
            response.header("Retry-After",String.valueOf(failure.retryAfterSeconds));
        }
        return response.body(Map.of("error",error));
    }
}
