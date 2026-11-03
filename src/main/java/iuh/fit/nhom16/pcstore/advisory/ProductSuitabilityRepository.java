package iuh.fit.nhom16.pcstore.advisory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductSuitabilityRepository extends JpaRepository<ProductSuitability, UUID> {

    @Query("SELECT ps FROM ProductSuitability ps WHERE ps.needProfile = :needProfile " +
           "AND (:maxBudget IS NULL OR ps.product.price <= :maxBudget) " +
           "ORDER BY ps.score DESC")
    List<ProductSuitability> findRecommendations(
            @Param("needProfile") String needProfile,
            @Param("maxBudget") BigDecimal maxBudget
    );
}
