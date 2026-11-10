package iuh.fit.nhom16.pcstore.tracking.repository;

import iuh.fit.nhom16.pcstore.tracking.entity.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {

    /** Recommendation reads are user-scoped and derive categories from real products,
     * not untrusted categorySlug metadata or another user's browser session. */
    @Query("""
            SELECT p.category.slug FROM UserActivityLog l JOIN Product p ON p.id=l.productId
            WHERE l.userId=:userId AND l.eventType='VIEW_PRODUCT' AND l.createdAt>=:since
              AND p.status='ACTIVE'
              AND p.category.slug IN ('cpu','mainboard','vga','ram','storage','cooling','case','psu','monitor','mouse','keyboard')
            GROUP BY p.category.slug ORDER BY COUNT(l) DESC, MAX(l.createdAt) DESC, p.category.slug ASC
            """)
    List<String> findTopViewedCategoriesByUser(@Param("userId") UUID userId,
            @Param("since") OffsetDateTime since, Pageable pageable);

    @Query("""
            SELECT l.categorySlug
            FROM UserActivityLog l
            WHERE l.categorySlug IS NOT NULL
              AND (((:userId IS NOT NULL) AND l.userId = :userId) OR l.sessionId = :sessionId)
              AND l.createdAt >= :since
            GROUP BY l.categorySlug
            ORDER BY COUNT(l) DESC
            """)
    List<String> findTopCategoriesBySessionOrUser(
            @Param("sessionId") String sessionId,
            @Param("userId") UUID userId,
            @Param("since") OffsetDateTime since
    );

    @Query("""
            SELECT l.productId
            FROM UserActivityLog l
            WHERE l.productId IS NOT NULL
              AND (((:userId IS NOT NULL) AND l.userId = :userId) OR l.sessionId = :sessionId)
              AND l.createdAt >= :since
            GROUP BY l.productId
            ORDER BY COUNT(l) DESC
            """)
    List<UUID> findTopProductIdsBySessionOrUser(
            @Param("sessionId") String sessionId,
            @Param("userId") UUID userId,
            @Param("since") OffsetDateTime since
    );

    List<UserActivityLog> findTop20BySessionIdOrUserIdOrderByCreatedAtDesc(String sessionId, UUID userId);

    long countBySessionId(String sessionId);

    long countByUserId(UUID userId);
}

