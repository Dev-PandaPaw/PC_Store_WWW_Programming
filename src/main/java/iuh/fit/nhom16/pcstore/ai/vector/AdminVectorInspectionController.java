package iuh.fit.nhom16.pcstore.ai.vector;

import iuh.fit.nhom16.pcstore.ai.AiFailure;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/vector/documents")
public class AdminVectorInspectionController {
    private final ProductVectorInspectionService inspection;

    public AdminVectorInspectionController(ProductVectorInspectionService inspection) {
        this.inspection = inspection;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVectorInspectionService.DocumentPage> documents(
            @RequestParam(defaultValue = "0") int offset, @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(inspection.documents(offset, limit));
    }

    @ExceptionHandler(AiFailure.class)
    public ResponseEntity<?> handleFailure(AiFailure error) {
        return ResponseEntity.status(error.status).cacheControl(CacheControl.noStore())
                .body(Map.of("error", Map.of("code", error.code, "message", error.getMessage())));
    }
}
