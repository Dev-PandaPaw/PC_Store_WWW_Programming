package iuh.fit.nhom16.pcstore.catalog;

import iuh.fit.nhom16.pcstore.catalog.dto.ProductRequest;
import iuh.fit.nhom16.pcstore.catalog.dto.ProductSpecRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;

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
    public Page<Product> getProductsPage(Pageable pageable) {
        return productRepository.findByStatus("ACTIVE", pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProductsByKeyword(String keyword, Pageable pageable) {
        return productRepository.searchActiveByKeyword(requireFilter(keyword, "keyword"), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByCategory(String categorySlug, Pageable pageable) {
        return productRepository.findByStatusAndCategory_SlugIgnoreCase(
                "ACTIVE", requireFilter(categorySlug, "category"), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByBrand(String brand, Pageable pageable) {
        return productRepository.findByStatusAndBrandContainingIgnoreCase(
                "ACTIVE", requireFilter(brand, "brand"), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        if (minPrice == null || maxPrice == null) {
            throw new IllegalArgumentException("minPrice và maxPrice là bắt buộc");
        }
        if (minPrice.signum() < 0 || maxPrice.signum() < 0 || minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Khoảng giá không hợp lệ");
        }
        return productRepository.findByStatusAndPriceBetween("ACTIVE", minPrice, maxPrice, pageable);
    }

    private String requireFilter(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " không được để trống");
        }
        return value.trim();
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        ensureUniqueKeys(request, null);
        Product product = new Product();
        applyProductDetails(product, request);
        product.setStatus("ACTIVE");
        product.setStockOnHand(0);
        product.setReservedQuantity(0);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new CatalogNotFoundException("Không tìm thấy sản phẩm: " + id));
        ensureUniqueKeys(request, id);
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
