package iuh.fit.nhom16.pcstore.sales.infrastructure;

import iuh.fit.nhom16.pcstore.sales.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository tối giản cho Order entity.
 *
 * <p><strong>⚠ STUB TẠM THỜI — THÀNH VIÊN 3 (Ngân) SẼ MỞ RỘNG FILE NÀY.</strong>
 *
 * <p>PaymentService chỉ cần {@code findById} (kế thừa từ JpaRepository)
 * và {@code save} để cập nhật trạng thái đơn hàng sau khi VNPay xác nhận.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {
    // findById(UUID) và save(Order) được kế thừa từ JpaRepository — đủ cho PaymentService
}
