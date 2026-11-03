package iuh.fit.nhom16.pcstore.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CatalogController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/products")
    public List<Product> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepository.searchByKeyword(keyword.trim());
        }
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
            return productRepository.findByCategorySlug(category.trim());
        }
        return productRepository.findByStatus("ACTIVE");
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/by-slug/{slug}")
    public ResponseEntity<Product> getProductBySlug(@PathVariable String slug) {
        return productRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
