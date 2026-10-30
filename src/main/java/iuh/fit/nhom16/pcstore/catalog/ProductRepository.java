package iuh.fit.nhom16.pcstore.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("""
            SELECT new iuh.fit.nhom16.pcstore.catalog.CartProductSnapshot(
                p.id, p.sku, p.name, p.price, p.status, p.stockOnHand, p.reservedQuantity)
            FROM Product p WHERE p.id IN :ids
            """)
    List<CartProductSnapshot> findCartProducts(@Param("ids") Collection<UUID> ids);

    Optional<Product> findBySlug(String slug);
    List<Product> findByCategorySlug(String categorySlug);
    List<Product> findByStatus(String status);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);
}
