package iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence;

import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.QueryHint;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    interface CartProductRow {
        UUID getId();
        String getSku();
        String getName();
        BigDecimal getPrice();
        String getStatus();
        Integer getStockOnHand();
        Integer getReservedQuantity();
    }

    @Query("""
            SELECT new iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot(
                p.id, p.sku, p.name, p.price, p.status, p.stockOnHand, p.reservedQuantity)
            FROM Product p WHERE p.id IN :ids
            """)
    List<CartProductSnapshot> findCartProducts(@Param("ids") Collection<UUID> ids);

    /**
     * Acquires PostgreSQL FOR SHARE row locks in deterministic ID order so concurrent stock updates
     * (checkout reservations / admin inventory changes) cannot interleave between cart stock check and commit,
     * while still allowing concurrent cart additions on the same product without blocking each other.
     */
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "3000"))
    @Query(value = """
            SELECT p.id AS id, p.sku AS sku, p.name AS name, p.price AS price,
                   p.status AS status, p.stock_on_hand AS stockOnHand, p.reserved_quantity AS reservedQuantity
            FROM products p
            WHERE p.id IN (:ids)
            ORDER BY p.id
            FOR SHARE
            """, nativeQuery = true)
    List<CartProductRow> lockCartProductRows(@Param("ids") Collection<UUID> ids);

    default List<CartProductSnapshot> lockCartProducts(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return lockCartProductRows(ids).stream()
                .map(row -> new CartProductSnapshot(
                        row.getId(),
                        row.getSku(),
                        row.getName(),
                        row.getPrice(),
                        row.getStatus(),
                        row.getStockOnHand(),
                        row.getReservedQuantity()))
                .toList();
    }

    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySkuIgnoreCase(String sku);
    Optional<Product> findBySlugIgnoreCase(String slug);
    List<Product> findByCategorySlug(String categorySlug);
    List<Product> findByStatus(String status);
    List<Product> findAllByOrderByUpdatedAtDesc();

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))) ")
    List<Product> searchByKeyword(@Param("keyword") String keyword);
}
