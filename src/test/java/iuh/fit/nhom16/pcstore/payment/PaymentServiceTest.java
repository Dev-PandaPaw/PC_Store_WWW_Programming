package iuh.fit.nhom16.pcstore.payment;

import iuh.fit.nhom16.pcstore.payment.application.PaymentService;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayConfig;
import iuh.fit.nhom16.pcstore.payment.infrastructure.vnpay.VNPayUtil;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private static final String HASH_SECRET = "OLTADMKAFDQDSFLAKWATGGNEGCIAPJQW";

    private VNPayConfig vnPayConfig;
    private JdbcTemplate jdbc;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        vnPayConfig = new VNPayConfig();
        vnPayConfig.setTmnCode("YZEE4138");
        vnPayConfig.setHashSecret(HASH_SECRET);
        vnPayConfig.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnPayConfig.setReturnUrl("http://localhost:5173/checkout/complete");
        vnPayConfig.setApiUrl("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");

        jdbc = mock(JdbcTemplate.class);
        service = new PaymentService(vnPayConfig, jdbc);
    }

    @Test
    void vnPayUtilNormalizesIpv6LoopbackAndHashesUrlEncodedFieldsConsistently() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("0:0:0:0:0:0:0:1");
        assertEquals("127.0.0.1", VNPayUtil.getIpAddress(req));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "999000000");
        params.put("vnp_OrderInfo", "Thanh toan don hang PC-1234");
        params.put("vnp_ReturnUrl", "http://localhost:8080/api/v1/payments/vnpay/callback");

        String signedQuery = VNPayUtil.buildSignedQuery(params, HASH_SECRET);
        assertTrue(signedQuery.contains("vnp_OrderInfo=Thanh+toan+don+hang+PC-1234"));
        assertTrue(signedQuery.contains("vnp_SecureHash="));

        String hash = signedQuery.substring(signedQuery.indexOf("vnp_SecureHash=") + "vnp_SecureHash=".length());
        assertEquals(VNPayUtil.hashAllFields(params, HASH_SECRET), hash);
    }

    @Test
    void createVnPayPaymentUrlGeneratesSignedSandboxUrlAndPersistsAttempt() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String key = UUID.randomUUID().toString();

        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id", UUID.class)).thenReturn(orderId);
        when(rs.getString("code")).thenReturn("PC-TEST01");
        when(rs.getObject("customer_id", UUID.class)).thenReturn(customerId);
        when(rs.getBigDecimal("total")).thenReturn(new BigDecimal("1990000.00"));
        when(rs.getString("payment_method")).thenReturn("VNPAY");
        when(rs.getString("status")).thenReturn("PENDING_PAYMENT");
        when(rs.getString("payment_status")).thenReturn("UNPAID");
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.from(Instant.now().plus(20, ChronoUnit.MINUTES)));

        when(jdbc.query(contains("FROM sales_orders"), any(RowMapper.class), eq(orderId)))
                .thenAnswer(inv -> {
                    RowMapper<?> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbc.query(contains("FROM sales_payment_attempts"), any(RowMapper.class), eq(orderId)))
                .thenReturn(List.of());
        when(jdbc.queryForList(contains("FROM sales_payment_attempts WHERE reference"), eq(UUID.class), eq(key)))
                .thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String url = service.createVnPayPaymentUrl(customerId, orderId, new BigDecimal("1990000"), key, request);

        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(url.contains("vnp_Amount=199000000"));
        assertTrue(url.contains("vnp_TxnRef=" + key));
        assertTrue(url.contains("vnp_IpAddr=127.0.0.1"));
        assertTrue(url.contains("vnp_SecureHash="));

        verify(jdbc).update(contains("INSERT INTO sales_payment_attempts"),
                eq(orderId), eq(key), eq(new BigDecimal("1990000")), eq(url));
    }

    @Test
    void createVnPayPaymentUrlRejectsAnotherUsersOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID attackerId = UUID.randomUUID();

        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("id", UUID.class)).thenReturn(orderId);
        when(rs.getString("code")).thenReturn("PC-TEST02");
        when(rs.getObject("customer_id", UUID.class)).thenReturn(ownerId);
        when(rs.getBigDecimal("total")).thenReturn(new BigDecimal("1990000.00"));
        when(rs.getString("payment_method")).thenReturn("VNPAY");
        when(rs.getString("status")).thenReturn("PENDING_PAYMENT");
        when(rs.getString("payment_status")).thenReturn("UNPAID");
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.from(Instant.now().plus(20, ChronoUnit.MINUTES)));

        when(jdbc.query(contains("FROM sales_orders"), any(RowMapper.class), eq(orderId)))
                .thenAnswer(inv -> List.of(((RowMapper<?>) inv.getArgument(1)).mapRow(rs, 0)));

        OrderException ex = assertThrows(OrderException.class, () ->
                service.createVnPayPaymentUrl(attackerId, orderId, new BigDecimal("1990000"), UUID.randomUUID().toString(), new MockHttpServletRequest()));
        assertEquals("ORDER_NOT_FOUND", ex.code());
    }

    @Test
    void processVnPayCallbackUpdatesSalesOrderAndHistoryOnValidSignature() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String txnRef = UUID.randomUUID().toString();

        ResultSet attemptRs = mock(ResultSet.class);
        when(attemptRs.getObject("order_id", UUID.class)).thenReturn(orderId);
        when(attemptRs.getString("reference")).thenReturn(txnRef);
        when(attemptRs.getBigDecimal("amount")).thenReturn(new BigDecimal("1990000"));
        when(attemptRs.getString("status")).thenReturn("PENDING");
        when(attemptRs.getString("payment_url")).thenReturn("https://sandbox.vnpayment.vn/...");

        ResultSet orderRs = mock(ResultSet.class);
        when(orderRs.getObject("id", UUID.class)).thenReturn(orderId);
        when(orderRs.getString("code")).thenReturn("PC-TEST03");
        when(orderRs.getObject("customer_id", UUID.class)).thenReturn(customerId);
        when(orderRs.getBigDecimal("total")).thenReturn(new BigDecimal("1990000.00"));
        when(orderRs.getString("payment_method")).thenReturn("VNPAY");
        when(orderRs.getString("status")).thenReturn("PENDING_PAYMENT");
        when(orderRs.getString("payment_status")).thenReturn("UNPAID");
        when(orderRs.getTimestamp("expires_at")).thenReturn(Timestamp.from(Instant.now().plus(20, ChronoUnit.MINUTES)));

        when(jdbc.query(contains("FROM sales_payment_attempts"), any(RowMapper.class), eq(txnRef)))
                .thenAnswer(inv -> List.of(((RowMapper<?>) inv.getArgument(1)).mapRow(attemptRs, 0)));
        when(jdbc.query(contains("FROM sales_orders"), any(RowMapper.class), eq(orderId)))
                .thenAnswer(inv -> List.of(((RowMapper<?>) inv.getArgument(1)).mapRow(orderRs, 0)));

        Map<String, String> callbackParams = new HashMap<>();
        callbackParams.put("vnp_Amount", "199000000");
        callbackParams.put("vnp_BankCode", "NCB");
        callbackParams.put("vnp_OrderInfo", "Thanh toan don hang PC-TEST03");
        callbackParams.put("vnp_ResponseCode", "00");
        callbackParams.put("vnp_TransactionStatus", "00");
        callbackParams.put("vnp_TransactionNo", "14567890");
        callbackParams.put("vnp_TxnRef", txnRef);
        callbackParams.put("vnp_SecureHash", VNPayUtil.hashAllFields(callbackParams, HASH_SECRET));

        PaymentService.CallbackResult result = service.processVnPayCallback(callbackParams);

        assertTrue(result.success());
        assertEquals(orderId, result.orderId());
        assertEquals("00", result.responseCode());
        assertEquals("14567890", result.transactionNo());

        verify(jdbc).update(contains("INSERT INTO sales_payment_events"),
                eq(orderId), eq("14567890"), eq("00"), eq("00"));
        verify(jdbc).update(contains("UPDATE sales_payment_attempts"),
                eq("14567890"), eq(orderId));
        verify(jdbc).update(contains("SET status = 'CONFIRMED', payment_status = 'PAID'"),
                eq(orderId));
        verify(jdbc).update(contains("INSERT INTO sales_history"),
                eq(orderId), eq("PENDING_PAYMENT"), eq("CONFIRMED"), contains("14567890"));
    }

    @Test
    void processVnPayCallbackRejectsTamperedSignature() {
        Map<String, String> callbackParams = new HashMap<>();
        callbackParams.put("vnp_TxnRef", UUID.randomUUID().toString());
        callbackParams.put("vnp_ResponseCode", "00");
        callbackParams.put("vnp_Amount", "100");
        callbackParams.put("vnp_SecureHash", "tampered_hash");

        PaymentService.CallbackResult result = service.processVnPayCallback(callbackParams);

        assertFalse(result.success());
        assertEquals("INVALID_SIGNATURE", result.errorType());
        verifyNoInteractions(jdbc);
    }
}
