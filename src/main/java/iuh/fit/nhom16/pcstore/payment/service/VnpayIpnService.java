package iuh.fit.nhom16.pcstore.payment.service;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.repository.OrderRepository;
import iuh.fit.nhom16.pcstore.sales.service.OrderNotification;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;

@Service
public class VnpayIpnService {
    private final VnpayGateway gateway;
    private final OrderRepository orders;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    public VnpayIpnService(VnpayGateway gateway, OrderRepository orders, ApplicationEventPublisher events, Clock clock) {
        this.gateway = gateway; this.orders = orders; this.events = events; this.clock = clock;
    }
    @Transactional
    public Map<String,String> handle(Map<String,String> params) {
        if (!gateway.verify(params)) return response("97", "Invalid signature");
        String reference = params.getOrDefault("vnp_TxnRef", "");
        var id = orders.findPayment(reference);
        if (id.isEmpty()) return response("01", "Order not found");
        Order order = orders.get(id.get(), true);
        if (order.paymentMethod() != PaymentMethod.VNPAY) return response("01", "Order not found");
        try {
            if (!params.getOrDefault("vnp_Amount", "").matches("[0-9]{1,22}") ||
                    order.total().multiply(BigDecimal.valueOf(100)).compareTo(new BigDecimal(params.get("vnp_Amount"))) != 0)
                return response("04", "Invalid amount");
        } catch (NumberFormatException ex) { return response("04", "Invalid amount"); }
        if ("PAID".equals(order.paymentStatus()) || "REVIEW_REQUIRED".equals(order.paymentStatus()) || "FAILED".equals(order.paymentStatus()))
            return response("02", "Order already confirmed");
        String transaction = params.getOrDefault("vnp_TransactionNo", "");
        String responseCode = params.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = params.getOrDefault("vnp_TransactionStatus", "");
        if (!transaction.matches("[0-9]{1,100}") || !responseCode.matches("[0-9]{2}") || !transactionStatus.matches("[0-9]{2}"))
            return response("99", "Invalid payment data");
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);
        if (!success && order.status() != OrderStatus.PENDING_PAYMENT) return response("02", "Order already confirmed");
        orders.paymentEvent(order.id(), transaction, responseCode, transactionStatus);
        boolean pending = order.status() == OrderStatus.PENDING_PAYMENT && clock.instant().isBefore(order.expiresAt());
        if (success && pending) {
            orders.payment(order, "PAID", transaction);
            orders.state(order, OrderStatus.PENDING_CONFIRMATION, "HELD");
            orders.history(order.id(), order.status().name(), "PENDING_CONFIRMATION", null, "VNPAY IPN xác minh thành công");
            events.publishEvent(new OrderNotification(order.email(), order.code(), "PAID"));
        } else if (success) {
            // A late payment must never resurrect cancelled/expired inventory. Admin reconciles/refunds it.
            orders.payment(order, "REVIEW_REQUIRED", transaction);
            if (order.status() == OrderStatus.PENDING_PAYMENT) {
                for (Line line : order.items()) { orders.lockProduct(line.productId(), line.quantity()); orders.reserve(line.productId(), -line.quantity()); }
                orders.state(order, OrderStatus.EXPIRED, "RELEASED");
                orders.history(order.id(), order.status().name(), "EXPIRED", null, "VNPAY đến sau hạn giữ hàng");
            }
            orders.history(order.id(), order.status().name(), order.status().name(), null, "VNPAY thanh toán muộn: cần đối soát/hoàn tiền");
        } else if (order.status() == OrderStatus.PENDING_PAYMENT) {
            for (Line line : order.items()) { orders.lockProduct(line.productId(), line.quantity()); orders.reserve(line.productId(), -line.quantity()); }
            orders.payment(order, "FAILED", transaction);
            orders.state(order, OrderStatus.CANCELLED, "RELEASED");
            orders.history(order.id(), order.status().name(), "CANCELLED", null, "VNPAY thanh toán thất bại");
        }
        return response("00", "Confirm Success");
    }
    private Map<String,String> response(String code, String message) { return Map.of("RspCode", code, "Message", message); }
}
