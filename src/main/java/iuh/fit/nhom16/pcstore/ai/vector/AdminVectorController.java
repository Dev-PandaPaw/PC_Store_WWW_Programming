package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Điểm nối cho nút Đồng bộ Vector AI của mục 3.5. */
@RestController
@RequestMapping("/api/admin/vector")
public class AdminVectorController {
    private final ProductVectorIngestionService ingestion;

    public AdminVectorController(ProductVectorIngestionService ingestion) { this.ingestion = ingestion; }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVectorIngestionService.ReindexResult> reindex() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ingestion.reindexAll());
    }

    @ExceptionHandler(AiFailure.class)
    public ResponseEntity<?> handleFailure(AiFailure error) {
        var response = ResponseEntity.status(error.status).cacheControl(CacheControl.noStore());
        if (error.retryAfterSeconds > 0) response.header("Retry-After", String.valueOf(error.retryAfterSeconds));
        return response.body(Map.of("error", Map.of("code", error.code, "message", error.getMessage())));
    }
}
