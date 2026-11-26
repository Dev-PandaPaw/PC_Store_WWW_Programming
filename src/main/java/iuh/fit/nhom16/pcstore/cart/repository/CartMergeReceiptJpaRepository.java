package iuh.fit.nhom16.pcstore.cart.repository;

import iuh.fit.nhom16.pcstore.cart.entity.CartMergeReceiptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CartMergeReceiptJpaRepository extends JpaRepository<CartMergeReceiptEntity, UUID> {

    @Modifying
    @Query(value = "INSERT INTO cart_merge_receipts(guest_id, cart_id) VALUES (:guestId, :cartId) ON CONFLICT (guest_id) DO NOTHING", nativeQuery = true)
    int claimIfAbsent(@Param("guestId") UUID guestId, @Param("cartId") UUID cartId);
}
