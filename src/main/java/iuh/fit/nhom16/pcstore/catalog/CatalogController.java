package iuh.fit.nhom16.pcstore.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogService catalogService;

    public CatalogController(CategoryRepository categoryRepository, ProductRepository productRepository,
                             CatalogService catalogService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.catalogService = catalogService;
    }

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return catalogService.getAllProducts();
    }

    @GetMapping("/products/page")
    public Page<Product> getProductsPage(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "12") int size,
                                         @RequestParam(defaultValue = "createdAt") String sortBy,
                                         @RequestParam(defaultValue = "desc") String sortDirection) {
        return catalogService.getProductsPage(pageable(page, size, sortBy, sortDirection));
    }

    @GetMapping("/products/search")
    public Page<Product> searchProducts(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "12") int size,
                                        @RequestParam(defaultValue = "createdAt") String sortBy,
                                        @RequestParam(defaultValue = "desc") String sortDirection) {
        return catalogService.searchProductsByKeyword(keyword, pageable(page, size, sortBy, sortDirection));
    }

    @GetMapping("/products/by-category/{categorySlug}")
    public Page<Product> getProductsByCategory(@PathVariable String categorySlug,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "12") int size,
                                               @RequestParam(defaultValue = "createdAt") String sortBy,
                                               @RequestParam(defaultValue = "desc") String sortDirection) {
        return catalogService.getProductsByCategory(categorySlug, pageable(page, size, sortBy, sortDirection));
    }

    @GetMapping("/products/by-brand")
    public Page<Product> getProductsByBrand(@RequestParam String brand,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size,
                                            @RequestParam(defaultValue = "createdAt") String sortBy,
                                            @RequestParam(defaultValue = "desc") String sortDirection) {
        return catalogService.getProductsByBrand(brand, pageable(page, size, sortBy, sortDirection));
    }

    @GetMapping("/products/by-price-range")
    public Page<Product> getProductsByPriceRange(@RequestParam BigDecimal minPrice,
                                                 @RequestParam BigDecimal maxPrice,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "12") int size,
                                                 @RequestParam(defaultValue = "price") String sortBy,
                                                 @RequestParam(defaultValue = "asc") String sortDirection) {
        return catalogService.getProductsByPriceRange(minPrice, maxPrice,
                pageable(page, size, sortBy, sortDirection));
    }

    private PageRequest pageable(int page, int size, String sortBy, String sortDirection) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page phải từ 0 trở lên và size phải từ 1 đến 100");
        }
        if (!Set.of("name", "price", "createdAt").contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortBy chỉ nhận name, price hoặc createdAt");
        }
        try {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortDirection chỉ nhận asc hoặc desc");
        }
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
