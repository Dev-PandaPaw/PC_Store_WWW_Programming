package iuh.fit.nhom16.pcstore.ai;

import iuh.fit.nhom16.pcstore.advisory.AiAdvisorController;
import iuh.fit.nhom16.pcstore.advisory.HardwareAttachmentController;
import iuh.fit.nhom16.pcstore.catalog.web.SmartSearchController;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(-10)
@RestControllerAdvice(assignableTypes = {AiAdvisorController.class, HardwareAttachmentController.class, SmartSearchController.class})
public class AiExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<?> missingFile(org.springframework.web.multipart.support.MissingServletRequestPartException error) {
        return handle(new AiFailure(400,"EMPTY_ATTACHMENT","Hãy chọn ảnh hoặc file thông số có nội dung."));
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> oversized(org.springframework.web.multipart.MaxUploadSizeExceededException error) {
        return handle(new AiFailure(413,"ATTACHMENT_TOO_LARGE","Mỗi file tối đa 5 MB."));
    }
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
