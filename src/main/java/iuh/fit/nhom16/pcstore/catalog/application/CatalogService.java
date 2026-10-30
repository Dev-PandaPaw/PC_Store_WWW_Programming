package iuh.fit.nhom16.pcstore.catalog.application;

import iuh.fit.nhom16.pcstore.catalog.api.dto.ProductRequest;
import iuh.fit.nhom16.pcstore.catalog.api.dto.ProductSpecRequest;
import iuh.fit.nhom16.pcstore.catalog.domain.CatalogConflictException;
import iuh.fit.nhom16.pcstore.catalog.domain.CatalogNotFoundException;
import iuh.fit.nhom16.pcstore.catalog.domain.Category;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.domain.ProductSpec;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.CategoryRepository;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "DISCONTINUED");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CatalogService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findByStatus("ACTIVE");
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProductsForAdmin() {
        return productRepository.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(UUID id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductBySlug(String slug) {
        return productRepository.findBySlug(slug);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        ensureUniqueKeys(request, null);
        Product product = new Product();
        applyProductDetails(product, request);
        product.setStatus("ACTIVE");
        product.setReservedQuantity(0);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Không tìm thấy sản phẩm: " + id));
        ensureUniqueKeys(request, id);
        if (request.stockOnHand() < product.getReservedQuantity()) {
            throw new IllegalArgumentException("Stock cannot be lower than the quantity already reserved for orders");
        }
        applyProductDetails(product, request);
        product.setUpdatedAt(OffsetDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProductStatus(UUID id, String requestedStatus) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Không tìm thấy sản phẩm: " + id));
        String status = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Trạng thái hợp lệ: " + String.join(", ", ALLOWED_STATUSES));
        }
        product.setStatus(status);
        product.setUpdatedAt(OffsetDateTime.now());
        return productRepository.save(product);
    }

    private void applyProductDetails(Product product, ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CatalogNotFoundException("Không tìm thấy danh mục: " + request.categoryId()));

        product.setCategory(category);
        product.setSku(request.sku().trim());
        product.setSlug(request.slug().trim());
        product.setName(request.name().trim());
        product.setBrand(normalizeOptional(request.brand()));
        product.setDescription(normalizeOptional(request.description()));
        product.setPrice(request.price());
        product.setStockOnHand(request.stockOnHand());
        product.setWarrantyMonths(request.warrantyMonths() == null ? 12 : request.warrantyMonths());
        applySpec(product, request.spec());
    }

    private void applySpec(Product product, ProductSpecRequest request) {
        if (request == null) {
            product.setSpec(null);
            return;
        }

        ProductSpec spec = product.getSpec();
        if (spec == null) {
            spec = new ProductSpec();
            spec.setProduct(product);
            product.setSpec(spec);
        }
        spec.setCpu(normalizeOptional(request.cpu()));
        spec.setGpu(normalizeOptional(request.gpu()));
        spec.setRamGb(request.ramGb());
        spec.setStorageGb(request.storageGb());
        spec.setStorageType(normalizeOptional(request.storageType()));
        spec.setMotherboard(normalizeOptional(request.motherboard()));
        spec.setPsu(normalizeOptional(request.psu()));
        spec.setCaseName(normalizeOptional(request.caseName()));
        spec.setOperatingSystem(normalizeOptional(request.operatingSystem()));
    }

    private void ensureUniqueKeys(ProductRequest request, UUID productId) {
        boolean duplicateSku = productRepository.findBySkuIgnoreCase(request.sku().trim())
                .filter(existing -> !existing.getId().equals(productId)).isPresent();
        boolean duplicateSlug = productRepository.findBySlugIgnoreCase(request.slug().trim())
                .filter(existing -> !existing.getId().equals(productId)).isPresent();
        if (duplicateSku) throw new CatalogConflictException("SKU đã được sử dụng");
        if (duplicateSlug) throw new CatalogConflictException("Slug đã được sử dụng");
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
