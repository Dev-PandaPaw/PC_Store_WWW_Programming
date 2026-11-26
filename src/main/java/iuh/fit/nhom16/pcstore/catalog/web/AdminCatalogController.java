package iuh.fit.nhom16.pcstore.catalog.web;

import iuh.fit.nhom16.pcstore.catalog.api.dto.ProductRequest;
import iuh.fit.nhom16.pcstore.catalog.api.dto.ProductStatusRequest;
import iuh.fit.nhom16.pcstore.catalog.application.CatalogService;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/admin/catalog/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {
    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<Product> getProducts() {
        return catalogService.getAllProductsForAdmin();
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product created = catalogService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return catalogService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/status")
    public Product updateProductStatus(@PathVariable UUID id,
                                       @Valid @RequestBody ProductStatusRequest request) {
        return catalogService.updateProductStatus(id, request.status());
    }
}
