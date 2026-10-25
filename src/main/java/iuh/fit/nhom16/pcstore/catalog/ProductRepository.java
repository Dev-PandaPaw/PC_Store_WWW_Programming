package iuh.fit.nhom16.pcstore.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySkuIgnoreCase(String sku);
    Optional<Product> findBySlugIgnoreCase(String slug);
    List<Product> findByStatus(String status);
    Page<Product> findByStatus(String status, Pageable pageable);
    Page<Product> findByStatusAndCategory_SlugIgnoreCase(String status, String slug, Pageable pageable);
    Page<Product> findByStatusAndBrandContainingIgnoreCase(String status, String brand, Pageable pageable);
    Page<Product> findByStatusAndPriceBetween(String status, BigDecimal minPrice, BigDecimal maxPrice,
                                               Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = 'ACTIVE' AND
                  (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Product> searchActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
