package iuh.fit.nhom16.pcstore.cart.repository;

import iuh.fit.nhom16.pcstore.cart.entity.CartEntity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartJpaRepository extends JpaRepository<CartEntity, UUID> {

    @Modifying
    @Query(value = "INSERT INTO carts(id, user_id) VALUES (:id, :userId) ON CONFLICT (user_id) DO NOTHING", nativeQuery = true)
    int ensureCartExists(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "3000"))
    @Query("SELECT c FROM CartEntity c WHERE c.userId = :userId")
    Optional<CartEntity> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT c FROM CartEntity c LEFT JOIN FETCH c.items WHERE c.userId = :userId")
    Optional<CartEntity> findSnapshotByUserId(@Param("userId") UUID userId);
}
