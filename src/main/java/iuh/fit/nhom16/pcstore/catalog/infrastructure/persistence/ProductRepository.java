package iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence;

import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySkuIgnoreCase(String sku);
    Optional<Product> findBySlugIgnoreCase(String slug);
    List<Product> findByStatus(String status);
    List<Product> findAllByOrderByUpdatedAtDesc();
}
