package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.pcbuild.PcBuildCheckoutGuard;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.repository.OrderRepository;
import iuh.fit.nhom16.pcstore.sales.security.OrderActorResolver;
import iuh.fit.nhom16.pcstore.sales.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    final OrderRepository orders = mock(OrderRepository.class);
    final CartRepository carts = mock(CartRepository.class);
    final OrderActorResolver actors = mock(OrderActorResolver.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final Instant now = Instant.parse("2026-10-28T08:00:00Z");
    final UUID actor = UUID.randomUUID();
    final UUID product = UUID.randomUUID();
    final UUID secondProduct = UUID.randomUUID();
    final UUID id = UUID.randomUUID();
    final UUID quoteId = UUID.randomUUID();
    final Line line = new Line(product, "PC", "PC-01", BigDecimal.valueOf(1_000_000), 2, 36);
    final Line secondLine = new Line(secondProduct, "Monitor", "MON-01", BigDecimal.valueOf(3_000_000), 1, 24);
    final CartSession cart = new CartSession(UUID.randomUUID(), 3, Map.of(product, 2));
    final PcBuildCheckoutGuard buildCheckout = mock(PcBuildCheckoutGuard.class);
    final OrderService service = new OrderService(orders, carts, actors, events, Clock.fixed(now, ZoneOffset.UTC), buildCheckout);

    @BeforeEach
    void setup() {
        when(actors.require(eq(actor), anyString())).thenReturn(
                new Account(actor, "Customer", "demo@example.test", "+84912345678", true, List.of("CUSTOMER", "ADMIN")));
        when(carts.lock(actor)).thenReturn(cart);
        when(orders.lockProduct(product, 2)).thenReturn(new OrderRepository.Stock(line, 5, "ACTIVE", 1L));
        when(orders.lockProduct(product, 3)).thenReturn(new OrderRepository.Stock(line, 5, "ACTIVE", 1L));
        when(orders.lockProduct(secondProduct, 1)).thenReturn(new OrderRepository.Stock(secondLine, 4, "ACTIVE", 1L));
        when(orders.lockQuote(quoteId, actor)).thenReturn(quote(now.plusSeconds(600)));
        when(orders.replay(eq(actor), anyString(), any())).thenReturn(Optional.empty());
        when(orders.get(any(), anyBoolean())).thenReturn(
                order(OrderStatus.PENDING_CONFIRMATION, PaymentMethod.COD, "UNPAID", "HELD", now.plusSeconds(600)));
    }

    Quote quote(Instant expiry) {
        return new Quote(quoteId, actor, 3, "Customer", "+84912345678", "123 Street, Ward, City",
                expiry, List.of(line), OrderService.SHIPPING_FEE);
    }

    static Shipping validShipping() {
        return new Shipping("Customer", "0912345678", "Thành phố Hồ Chí Minh", "Phường Bến Nghé", "123 Nguyễn Huệ", "Quận 1");
    }

    Quote voucherQuote(Line quotedLine, String code, VoucherRuleSnapshot rule, BigDecimal discount) {
        BigDecimal subtotal = quotedLine.subtotal();
        BigDecimal shipping = BigDecimal.valueOf(50_000);
        return new Quote(quoteId, actor, 3, "Customer", "+84912345678", "123 Nguyễn Huệ, Phường Bến Nghé, Quận 1, Thành phố Hồ Chí Minh",
                79, 760, 26740, ShippingMethod.EXPRESS, PaymentMethod.COD, now.plusSeconds(600), List.of(quotedLine),
                subtotal, shipping, BigDecimal.ZERO, code, rule, discount, subtotal.add(shipping).subtract(discount));
    }

    Order voucherOrder(int quantity, VoucherRuleSnapshot rule, BigDecimal discount) {
        Line big = new Line(product, "PC", "PC-01", BigDecimal.valueOf(2_000_000), quantity, 36);
        BigDecimal subtotal = big.subtotal();
        BigDecimal shipping = BigDecimal.valueOf(50_000);
        return new Order(id, "PC-01", actor, "demo@example.test", "Customer", "+84912345678", "Address",
                79, 760, 26740, ShippingMethod.EXPRESS, PaymentMethod.COD, OrderStatus.PENDING_CONFIRMATION,
                "UNPAID", "HELD", 4, now.plusSeconds(600), now, subtotal, shipping, BigDecimal.ZERO,
                rule == null ? null : rule.code(), rule, discount, subtotal.add(shipping).subtract(discount),
                List.of(big), List.of());
    }

    static final VoucherRuleSnapshot PERCENT_5 = new VoucherRuleSnapshot(
            "PIXEL5", "PERCENT", new BigDecimal("5"), BigDecimal.valueOf(5_000_000), BigDecimal.valueOf(500_000));

    Order order(OrderStatus status, PaymentMethod method, String payment, String reservation, Instant expiry) {
        return new Order(id, "PC-01", actor, "demo@example.test", "Customer", "+84912345678", "Address", method,
                status, payment, reservation, 4, expiry, now, OrderService.SHIPPING_FEE, BigDecimal.valueOf(2_050_000), List.of(line), List.of());
    }

    @Test
    void vnpayOrderCreatesPendingPaymentOrderSeparatelyWithoutCallingGateway() {
        when(orders.get(any(), eq(false))).thenReturn(
                order(OrderStatus.PENDING_PAYMENT, PaymentMethod.VNPAY, "UNPAID", "HELD", now.plus(Duration.ofMinutes(30))));
        Order created = service.place(actor, "request-vnpay-1", new PlaceRequest(quoteId, PaymentMethod.VNPAY));
        assertEquals(OrderStatus.PENDING_PAYMENT, created.status());
        verify(orders).create(any(), any(), eq(PaymentMethod.VNPAY), eq(OrderStatus.PENDING_PAYMENT), anyString(), eq(now.plus(Duration.ofMinutes(30))));
    }

    @Test
    void quoteUsesServerPriceAndCalculatesSubtotalShippingAndVoucherDiscountWithBigDecimal() {
        OrderRepository.Voucher voucher = new OrderRepository.Voucher(
                "PIXEL5", "5% off", "PERCENT", new BigDecimal("5"),
                BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000),
                100, 0, 2, true, now.minusSeconds(3600), now.plusSeconds(3600), 0L);
        when(orders.findVoucher("PIXEL5")).thenReturn(Optional.of(voucher));
        when(orders.countCustomerVoucherUsage("PIXEL5", actor)).thenReturn(0);

        Quote result = service.quote(actor, new QuoteRequest(
                3,
                validShipping(),
                List.of(product),
                ShippingMethod.INSTANT,
                "pixel5",
                PaymentMethod.COD));

        // subtotal = 2,000,000; shipping (INSTANT) = 80,000; discount (5%) = 100,000; total = 1,980,000
        assertEquals(BigDecimal.valueOf(2_000_000), result.subtotal());
        assertEquals(BigDecimal.valueOf(80_000), result.shippingFee());
        assertEquals(BigDecimal.valueOf(100_000), result.discountAmount());
        assertEquals(BigDecimal.valueOf(1_980_000), result.total());
        assertEquals("PIXEL5", result.voucherCode());
        verify(orders, never()).reserve(any(), anyInt());
    }

    @Test
    void selectiveCheckoutLeavesUnselectedItemsAndNewQuantitiesFromOtherTabsIntact() {
        // Cart initially had product=2 and secondProduct=1; customer selected ONLY product=2 in quote.
        // Meanwhile in another browser tab, customer added +3 more of product (now 5) and kept secondProduct=1!
        CartSession multiTabCart = new CartSession(UUID.randomUUID(), 5, Map.of(product, 5, secondProduct, 1));
        when(carts.lock(actor)).thenReturn(multiTabCart);

        service.place(actor, "selective-123", new PlaceRequest(quoteId, PaymentMethod.COD));

        // Only the 2 ordered units of `product` are deducted (5 - 2 = 3); `secondProduct` (1) stays untouched!
        assertEquals(3, multiTabCart.quantities().get(product));
        assertEquals(1, multiTabCart.quantities().get(secondProduct));
        verify(carts).save(multiTabCart);
    }

    @Test
    void emptyCartOrEmptySelectionOrForeignItemAreRejected() {
        OrderException noSelection = assertThrows(OrderException.class, () -> service.quote(actor,
                new QuoteRequest(3, new Shipping("Customer", "0912345678", "Thành phố Hồ Chí Minh", "Phường Bến Nghé", "123 Nguyễn Huệ", "Quận 1"),
                        List.of(), ShippingMethod.EXPRESS, null, PaymentMethod.COD)));
        assertEquals("NO_ITEMS_SELECTED", noSelection.code());

        OrderException notInCart = assertThrows(OrderException.class, () -> service.quote(actor,
                new QuoteRequest(3, new Shipping("Customer", "0912345678", "Thành phố Hồ Chí Minh", "Phường Bến Nghé", "123 Nguyễn Huệ", "Quận 1"),
                        List.of(UUID.randomUUID()), ShippingMethod.EXPRESS, null, PaymentMethod.COD)));
        assertEquals("CART_ITEM_NOT_IN_CART", notInCart.code());

        when(carts.lock(actor)).thenReturn(new CartSession(UUID.randomUUID(), 0, Map.of()));
        OrderException empty = assertThrows(OrderException.class, () -> service.quote(actor,
                new QuoteRequest(0, new Shipping("Customer", "0912345678", "Thành phố Hồ Chí Minh", "Phường Bến Nghé", "123 Nguyễn Huệ", "Quận 1"))));
        assertEquals("EMPTY_CART", empty.code());
    }

    @Test
    void priceChangeBetweenQuoteAndPlacementRequiresReconfirmationWithRecalculatedTotal() {
        // Catalog price changed from 1,000,000 to 1,200,000 before user confirmed placement
        when(orders.lockProduct(product, 2)).thenReturn(
                new OrderRepository.Stock(new Line(product, "PC", "PC-01", BigDecimal.valueOf(1_200_000), 2, 36), 5, "ACTIVE", 2L));

        OrderException ex = assertThrows(OrderException.class,
                () -> service.place(actor, "price-change-1", new PlaceRequest(quoteId, PaymentMethod.COD)));

        assertEquals("PRICE_CHANGED", ex.code());
        assertEquals(true, ex.details().get("reconfirmationRequired"));
        PricingBreakdown recalculated = (PricingBreakdown) ex.details().get("recalculatedPricing");
        assertEquals(BigDecimal.valueOf(2_450_000), recalculated.total());
        verify(orders, never()).reserve(any(), anyInt());
        verify(orders, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void expiredVoucherAtPlacementRecalculatesTotalWithoutVoucherAndRequestsReconfirmation() {
        Quote voucherQuote = voucherQuote(line, "PIXEL50K",
                new VoucherRuleSnapshot("PIXEL50K", "FIXED", BigDecimal.valueOf(50_000), BigDecimal.valueOf(1_000_000), null),
                BigDecimal.valueOf(50_000));
        when(orders.lockQuote(quoteId, actor)).thenReturn(voucherQuote);

        // Voucher expired 1 second ago!
        OrderRepository.Voucher expiredVoucher = new OrderRepository.Voucher(
                "PIXEL50K", "50k off", "FIXED", BigDecimal.valueOf(50_000),
                BigDecimal.valueOf(1_000_000), null, 100, 0, 1, true,
                now.minusSeconds(7200), now.minusSeconds(1), 0L);
        when(orders.lockVoucher("PIXEL50K")).thenReturn(Optional.of(expiredVoucher));

        OrderException ex = assertThrows(OrderException.class,
                () -> service.place(actor, "voucher-exp-1", new PlaceRequest(quoteId, PaymentMethod.COD)));

        assertEquals("VOUCHER_RECONFIRMATION_REQUIRED", ex.code());
        assertEquals("PIXEL50K", ex.details().get("invalidVoucherCode"));
        PricingBreakdown recalculated = (PricingBreakdown) ex.details().get("recalculatedPricing");
        assertEquals(BigDecimal.ZERO, recalculated.discountAmount());
        assertEquals(BigDecimal.valueOf(2_050_000), recalculated.total());
        verify(orders, never()).reserve(any(), anyInt());
    }

    @Test
    void priceChangeRecalculatesPercentVoucherAgainstLiveSubtotalNotStaleDiscount() {
        VoucherRuleSnapshot rule = new VoucherRuleSnapshot(
                "PIXEL5", "PERCENT", new BigDecimal("5"), BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000));
        // Quoted: 2 x 1,000,000 = 2,000,000; discount 100,000
        when(orders.lockQuote(quoteId, actor)).thenReturn(voucherQuote(line, "PIXEL5", rule, BigDecimal.valueOf(100_000)));
        when(orders.lockVoucher("PIXEL5")).thenReturn(Optional.of(new OrderRepository.Voucher(
                "PIXEL5", "5% off", "PERCENT", new BigDecimal("5"), BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(500_000),
                100, 0, 2, true, now.minusSeconds(3600), now.plusSeconds(3600), 0L)));
        // Live price: 1,200,000 -> live subtotal 2,400,000 -> discount must become 120,000
        when(orders.lockProduct(product, 2)).thenReturn(
                new OrderRepository.Stock(new Line(product, "PC", "PC-01", BigDecimal.valueOf(1_200_000), 2, 36), 5, "ACTIVE", 2L));

        OrderException ex = assertThrows(OrderException.class,
                () -> service.place(actor, "price-voucher-1", new PlaceRequest(quoteId, PaymentMethod.COD)));

        assertEquals("PRICE_CHANGED", ex.code());
        PricingBreakdown recalculated = (PricingBreakdown) ex.details().get("recalculatedPricing");
        assertEquals(BigDecimal.valueOf(2_400_000), recalculated.subtotal());
        assertEquals(BigDecimal.valueOf(120_000), recalculated.discountAmount());
        assertEquals(BigDecimal.valueOf(2_330_000), recalculated.total());
        verify(orders, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminQuantityChangeRecalculatesPercentVoucherDiscountAndTotal() {
        // 5 x 2,000,000 = 10,000,000; 5% = 500,000 (cap 500,000); shipping 50,000; total 9,550,000
        when(orders.get(id, true)).thenReturn(voucherOrder(5, PERCENT_5, BigDecimal.valueOf(500_000)));

        service.quantity(actor, id, product, new QuantityRequest(4, 3, "Khách giảm số lượng"));

        var pricing = org.mockito.ArgumentCaptor.forClass(PricingBreakdown.class);
        verify(orders).updateItemQuantityAndPricing(any(), eq(product), eq(3), pricing.capture(), eq(PERCENT_5));
        assertEquals(BigDecimal.valueOf(6_000_000), pricing.getValue().subtotal());
        assertEquals(BigDecimal.valueOf(300_000), pricing.getValue().discountAmount());
        assertEquals(BigDecimal.valueOf(5_750_000), pricing.getValue().total());
        assertEquals("PIXEL5", pricing.getValue().voucherCode());
        verify(orders).updateVoucherUsageDiscount(id, BigDecimal.valueOf(300_000));
        verify(orders).reserve(product, -2);
        verify(orders, never()).releaseVoucher(any());
    }

    @Test
    void adminQuantityChangeBelowVoucherMinimumRemovesVoucherAndReleasesUsage() {
        when(orders.get(id, true)).thenReturn(voucherOrder(5, PERCENT_5, BigDecimal.valueOf(500_000)));

        service.quantity(actor, id, product, new QuantityRequest(4, 2, "Khách giảm số lượng"));

        var pricing = org.mockito.ArgumentCaptor.forClass(PricingBreakdown.class);
        verify(orders).updateItemQuantityAndPricing(any(), eq(product), eq(2), pricing.capture(), isNull());
        assertEquals(BigDecimal.valueOf(4_000_000), pricing.getValue().subtotal());
        assertEquals(BigDecimal.ZERO, pricing.getValue().discountAmount());
        assertNull(pricing.getValue().voucherCode());
        assertEquals(BigDecimal.valueOf(4_050_000), pricing.getValue().total());
        verify(orders).releaseVoucher(id);
        verify(orders, never()).updateVoucherUsageDiscount(any(), any());
    }

    @Test
    void quoteRejectsFakeOrMismatchedAdministrativeAddress() {
        OrderException fake = assertThrows(OrderException.class, () -> service.quote(actor, new QuoteRequest(3,
                new Shipping("Customer", "0912345678", "City", "Ward", "123 Street", "District"),
                List.of(product), ShippingMethod.EXPRESS, null, PaymentMethod.COD)));
        assertEquals("INVALID_SHIPPING_ADDRESS", fake.code());

        OrderException mismatch = assertThrows(OrderException.class, () -> service.quote(actor, new QuoteRequest(3,
                new Shipping("Customer", "0912345678", "Thành phố Hồ Chí Minh", "Phường Phúc Xá", "123 Street", "Quận 1"),
                List.of(product), ShippingMethod.EXPRESS, null, PaymentMethod.COD)));
        assertEquals("INVALID_SHIPPING_ADDRESS", mismatch.code());
        verify(orders, never()).saveQuote(any());
    }

    @Test
    void insufficientStockReportsAvailableQuantityAndDoesNotCreateOrder() {
        when(orders.lockProduct(product, 2)).thenReturn(new OrderRepository.Stock(line, 1, "ACTIVE", 1L));
        OrderException ex = assertThrows(OrderException.class,
                () -> service.place(actor, "stock-short-1", new PlaceRequest(quoteId, PaymentMethod.COD)));
        assertEquals("INSUFFICIENT_STOCK", ex.code());
        assertEquals(1, ex.details().get("availableQuantity"));
        verify(orders, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void adminAccountRequiresAdminRole() {
        assertEquals(actor, service.adminAccount(actor).id());
        verify(actors).require(actor, "ADMIN");
    }

    @Test
    void staleCartCannotQuote() {
        assertEquals("CART_VERSION_CONFLICT", assertThrows(OrderException.class, () -> service.quote(actor,
                new QuoteRequest(2, new Shipping("Customer", "0912345678", "Thành phố Hồ Chí Minh", "Phường Bến Nghé", "123 Nguyễn Huệ", "Quận 1")))).code());
        verify(orders, never()).saveQuote(any());
    }

    @Test
    void unverifiedPhoneCannotCheckout() {
        when(actors.require(actor, "CUSTOMER")).thenReturn(
                new Account(actor, "Customer", "mail", "+84912345678", false, List.of("CUSTOMER")));
        assertEquals("PHONE_NOT_VERIFIED", assertThrows(OrderException.class,
                () -> service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD))).code());
        verify(orders, never()).reserve(any(), anyInt());
    }

    @Test
    void expiredQuoteCannotReserve() {
        when(orders.lockQuote(quoteId, actor)).thenReturn(quote(now));
        assertEquals("QUOTE_EXPIRED", assertThrows(OrderException.class,
                () -> service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD))).code());
        verify(orders, never()).reserve(any(), anyInt());
    }

    @Test
    void replayAndTimeoutIdempotencyLookupReturnExistingOrderWithoutDuplicateSideEffects() {
        when(orders.replay(eq(actor), anyString(), any())).thenReturn(Optional.of(id));
        assertEquals(id, service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD)).id());
        verify(orders, never()).lockQuote(any(), any());
        verify(orders, never()).reserve(any(), anyInt());
        verify(carts, never()).save(any());

        when(orders.findOrderByIdempotencyKey(actor, "request-123")).thenReturn(Optional.of(id));
        assertTrue(service.checkIdempotencyResult(actor, "request-123").isPresent());
    }

    @Test
    void failedBuildGateCannotReserveCreateOrClearCart() {
        doThrow(OrderException.conflict("PC_BUILD_NOT_READY", "blocked")).when(buildCheckout).requireReady(any(), anyList());
        var error = assertThrows(OrderException.class, () -> service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD)));
        assertEquals("PC_BUILD_NOT_READY", error.code());
        verify(orders, never()).reserve(any(), anyInt());
        verify(orders, never()).create(any(), any(), any(), any(), any(), any());
        assertFalse(cart.quantities().isEmpty());
    }

    @Test
    void cancellationAndExpiryReleaseBothStockAndVoucher() {
        service.transition(actor, id, false, new TransitionRequest(4, OrderStatus.CANCELLED, "Cancel"));
        verify(orders).reserve(product, -2);
        verify(orders).releaseVoucher(id);
        verify(orders).state(any(), eq(OrderStatus.CANCELLED), eq("RELEASED"));
    }

    @Test
    void controlledLifecycleSupportsConfirmedProcessingShippingDeliveredAndCompleted() {
        when(orders.get(id, true)).thenReturn(order(OrderStatus.CONFIRMED, PaymentMethod.COD, "UNPAID", "HELD", now.minusSeconds(1)));
        service.transition(actor, id, true, new TransitionRequest(4, OrderStatus.PROCESSING, "Packing"));
        verify(orders).state(any(), eq(OrderStatus.PROCESSING), eq("HELD"));

        when(orders.get(id, true)).thenReturn(order(OrderStatus.PROCESSING, PaymentMethod.COD, "UNPAID", "HELD", now.minusSeconds(1)));
        service.transition(actor, id, true, new TransitionRequest(4, OrderStatus.SHIPPING, "Ship"));
        verify(orders).consume(product, 2);

        when(orders.get(id, true)).thenReturn(order(OrderStatus.SHIPPING, PaymentMethod.COD, "UNPAID", "CONSUMED", now.minusSeconds(1)));
        service.transition(actor, id, true, new TransitionRequest(4, OrderStatus.DELIVERED, "Delivered"));
        verify(orders).paymentStatus(any(), eq("PAID"));

        assertThrows(OrderException.class, () -> OrderStatus.PENDING_CONFIRMATION.requireTransition(OrderStatus.CONFIRMED, false));
        assertThrows(OrderException.class, () -> OrderStatus.DELIVERED.requireTransition(OrderStatus.CANCELLED, true));
        assertThrows(OrderException.class, () -> OrderStatus.COMPLETED.requireTransition(OrderStatus.CANCELLED, true));
    }

    @Test
    void customerCannotReadAnotherCustomersOrder() {
        UUID stranger = UUID.randomUUID();
        assertEquals(404, assertThrows(OrderException.class, () -> service.get(stranger, id, false)).status().value());
    }
}
