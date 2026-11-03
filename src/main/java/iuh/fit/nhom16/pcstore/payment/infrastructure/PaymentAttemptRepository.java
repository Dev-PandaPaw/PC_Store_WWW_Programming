package iuh.fit.nhom16.pcstore.payment.infrastructure;

import iuh.fit.nhom16.pcstore.payment.domain.PaymentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    /**
     * Tra cứu bản ghi theo mã tham chiếu VNPay (vnp_TxnRef).
     * Dùng PESSIMISTIC_WRITE lock để tránh race condition khi VNPay gửi nhiều callback đồng thời
     * cho cùng một giao dịch — đảm bảo chỉ một thread cập nhật trạng thái thành công.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pa FROM PaymentAttempt pa WHERE pa.paymentReference = :ref")
    Optional<PaymentAttempt> findByPaymentReferenceForUpdate(@Param("ref") String paymentReference);

    /**
     * Lấy tất cả lần thử thanh toán của một đơn hàng (không lock).
     * Dùng để hiển thị lịch sử thanh toán.
     */
    List<PaymentAttempt> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
