package iuh.fit.nhom16.pcstore.cart.repository;

import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CartAddOperationJpaRepository extends JpaRepository<CartAddOperationEntity, CartAddOperationId> {
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO cart_add_operations(cart_id, operation_id, product_id, quantity, result_version)
            VALUES (:cartId, :operationId, :productId, :quantity, :version)
            ON CONFLICT (cart_id, operation_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("cartId") UUID cartId, @Param("operationId") UUID operationId,
                       @Param("productId") UUID productId, @Param("quantity") int quantity,
                       @Param("version") long version);
}
