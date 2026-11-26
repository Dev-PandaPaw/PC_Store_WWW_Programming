package iuh.fit.nhom16.pcstore.identity.infrastructure;

import iuh.fit.nhom16.pcstore.identity.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, UUID id);

    boolean existsByIdAndStatusIgnoreCase(UUID id, String status);

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE UPPER(u.status) = 'ACTIVE' AND r IN ('ROLE_ADMIN', 'ADMIN')")
    long countActiveAdmins();

    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR :status = '' OR u.status = :status)")
    Page<User> searchUsers(@Param("query") String query, @Param("status") String status, Pageable pageable);
}
