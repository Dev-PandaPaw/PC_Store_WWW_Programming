package iuh.fit.nhom16.pcstore.cart.repository;

import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartAddOperationJpaRepository extends JpaRepository<CartAddOperationEntity, CartAddOperationId> {
}
