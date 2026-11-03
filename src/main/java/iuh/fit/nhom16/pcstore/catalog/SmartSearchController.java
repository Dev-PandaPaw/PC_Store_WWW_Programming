package iuh.fit.nhom16.pcstore.catalog;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/search")
public class SmartSearchController {
    private final SmartSearchService search;
    public SmartSearchController(SmartSearchService search) { this.search = search; }

    @GetMapping
    public ResponseEntity<SmartSearchService.SearchResult> search(
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "featured") String sort, @RequestParam(defaultValue = "false") boolean deals,
            HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(search.search(keyword,category,sort,deals,request.getRemoteAddr()));
    }
}
