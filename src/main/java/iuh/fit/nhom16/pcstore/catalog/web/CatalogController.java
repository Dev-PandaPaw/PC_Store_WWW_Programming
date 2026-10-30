package iuh.fit.nhom16.pcstore.catalog.web;

import iuh.fit.nhom16.pcstore.catalog.application.CatalogService;
import iuh.fit.nhom16.pcstore.catalog.domain.Category;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return catalogService.getAllCategories();
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return catalogService.getAllProducts();
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return catalogService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/by-slug/{slug}")
    public ResponseEntity<Product> getProductBySlug(@PathVariable String slug) {
        return catalogService.getProductBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
