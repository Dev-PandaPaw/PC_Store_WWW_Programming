package iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence;

import iuh.fit.nhom16.pcstore.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
}
