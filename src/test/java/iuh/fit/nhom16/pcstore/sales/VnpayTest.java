package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.payment.service.*;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VnpayTest {
    final VnpayGateway gateway = new VnpayGateway("MERCHANT", "test-secret", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "http://localhost:5173/orders");
    final OrderRepository orders = mock(OrderRepository.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final Instant now = Instant.parse("2026-10-28T08:00:00Z");
    final VnpayIpnService service = new VnpayIpnService(gateway, orders, events, Clock.fixed(now, ZoneOffset.UTC));
    final UUID id = UUID.randomUUID(), product = UUID.randomUUID();
    Map<String,String> parameters() {
        return new HashMap<>(Map.of("vnp_TmnCode", "MERCHANT", "vnp_TxnRef", "reference", "vnp_Amount", "25000000",
                "vnp_ResponseCode", "00", "vnp_TransactionStatus", "00", "vnp_TransactionNo", "123456"));
    }
    Map<String,String> signed(Map<String,String> params) { params.put("vnp_SecureHash", gateway.sign(params)); return params; }
    void order(OrderStatus status, String payment, Instant expiry) {
        when(orders.findPayment("reference")).thenReturn(Optional.of(id));
        when(orders.get(id, true)).thenReturn(new Order(id, "PC-1", UUID.randomUUID(), "demo@example.test", "Name", "Phone", "Address",
                PaymentMethod.VNPAY, status, payment, status == OrderStatus.EXPIRED ? "RELEASED" : "HELD", 0, expiry, now,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(250000), "reference", null,
                List.of(new Line(product, "PC", "SKU", BigDecimal.valueOf(100000), 2, 36)), List.of()));
    }
    @Test void invalidSignatureCannotAccessOrder() {
        assertEquals("97", service.handle(parameters()).get("RspCode")); verifyNoInteractions(orders);
    }
    @Test void tamperingWithSignedAmountFailsVerification() {
        var params = signed(parameters()); params.put("vnp_Amount", "1");
        assertFalse(gateway.verify(params));
    }
    @Test void signatureMatchesIndependentHmacVectorWithSortedFormEncoding() {
        var params = Map.of("vnp_Amount", "25000000", "vnp_OrderInfo", "Thanh toan PC", "vnp_TmnCode", "MERCHANT", "vnp_TxnRef", "reference");
        assertEquals("bbef42c0a77702cd6c36dfd22c19e40887ae0e58257c1132059aac8751d17334fa35146f45a44eb53f02ad2f263f829e5fcb54e456455e29e5a42c1a978abafb", gateway.sign(params));
    }
    @Test void signedWrongAmountCannotMarkPaid() {
        order(OrderStatus.PENDING_PAYMENT, "UNPAID", now.plusSeconds(100));
        var params = parameters(); params.put("vnp_Amount", "1");
        assertEquals("04", service.handle(signed(params)).get("RspCode"));
        verify(orders, never()).payment(any(), any(), any());
    }
    @Test void successfulIpnMarksPaidAndWaitsForAdmin() {
        order(OrderStatus.PENDING_PAYMENT, "UNPAID", now.plusSeconds(100));
        assertEquals("00", service.handle(signed(parameters())).get("RspCode"));
        verify(orders).payment(any(), eq("PAID"), eq("123456"));
        verify(orders).state(any(), eq(OrderStatus.PENDING_CONFIRMATION), eq("HELD"));
    }
    @Test void replayDoesNotApplyPaymentTwice() {
        order(OrderStatus.PENDING_CONFIRMATION, "PAID", now.plusSeconds(100));
        assertEquals("02", service.handle(signed(parameters())).get("RspCode"));
        verify(orders, never()).payment(any(), any(), any());
    }
    @Test void latePaymentDoesNotResurrectExpiredOrder() {
        order(OrderStatus.EXPIRED, "EXPIRED", now.minusSeconds(1));
        assertEquals("00", service.handle(signed(parameters())).get("RspCode"));
        verify(orders).payment(any(), eq("REVIEW_REQUIRED"), any());
        verify(orders, never()).reserve(any(), anyInt()); verify(orders, never()).state(any(), any(), any());
    }
    @Test void latePaymentBeforeExpiryJobReleasesStockAndRequiresReview() {
        order(OrderStatus.PENDING_PAYMENT, "UNPAID", now.minusSeconds(1));
        service.handle(signed(parameters()));
        verify(orders).reserve(product, -2); verify(orders).state(any(), eq(OrderStatus.EXPIRED), eq("RELEASED"));
        verify(orders).payment(any(), eq("REVIEW_REQUIRED"), any());
    }
    @Test void failureReleasesHeldStock() {
        order(OrderStatus.PENDING_PAYMENT, "UNPAID", now.plusSeconds(100));
        var params = parameters(); params.put("vnp_ResponseCode", "24"); params.put("vnp_TransactionStatus", "02");
        service.handle(signed(params));
        verify(orders).reserve(product, -2); verify(orders).state(any(), eq(OrderStatus.CANCELLED), eq("RELEASED"));
    }
    @Test void paymentUrlUsesAmountTimes100AndVietnamTime() {
        String url = gateway.paymentUrl(id, BigDecimal.valueOf(250000), now, now.plusSeconds(600), "127.0.0.1");
        assertTrue(url.contains("vnp_Amount=25000000")); assertTrue(url.contains("vnp_CreateDate=20261028150000"));
        assertTrue(url.contains("vnp_ExpireDate=20261028151000")); assertTrue(url.contains("vnp_SecureHash="));
    }
}
