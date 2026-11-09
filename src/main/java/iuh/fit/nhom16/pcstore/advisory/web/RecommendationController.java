package iuh.fit.nhom16.pcstore.advisory.web;

import iuh.fit.nhom16.pcstore.advisory.application.ProductSemanticRecommendationService;
import iuh.fit.nhom16.pcstore.advisory.application.RecommendationFailure;
import iuh.fit.nhom16.pcstore.advisory.infrastructure.ProductSemanticIndexService;
import iuh.fit.nhom16.pcstore.identity.api.RecommendationAccountAccess;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
public class RecommendationController {
    private final ProductSemanticRecommendationService recommendations;
    private final RecommendationAccountAccess accounts;
    private final ObjectProvider<ProductSemanticIndexService> index;
    public RecommendationController(ProductSemanticRecommendationService recommendations,RecommendationAccountAccess accounts,
            ObjectProvider<ProductSemanticIndexService> index) {
        this.recommendations=recommendations; this.accounts=accounts; this.index=index;
    }
    @GetMapping("/api/v1/recommendations/personalized")
    public ResponseEntity<?> personalized(Authentication authentication,
            @RequestParam(defaultValue="") List<UUID> selectedProductIds) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(recommendations.personalized(accounts.requireActiveUser(authentication),selectedProductIds));
    }
    @GetMapping("/api/v1/recommendations/similar/{productId}")
    public ResponseEntity<?> similar(@PathVariable UUID productId,
            @RequestParam(defaultValue="") List<UUID> selectedProductIds) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recommendations.similar(productId,selectedProductIds));
    }
    @PostMapping("/api/v1/admin/recommendations/reindex")
    public ResponseEntity<?> reindex(Authentication authentication) {
        accounts.requireActiveUser(authentication);
        var service=index.getIfAvailable();
        if (service==null) throw new RecommendationFailure(503,"VECTOR_UNAVAILABLE","Tìm kiếm vector chưa được bật.");
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.synchronize()); }
        catch (RuntimeException e) { throw new RecommendationFailure(503,"INDEX_UNAVAILABLE","Chưa đồng bộ được chỉ mục vector."); }
    }
}
