package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.payment.service.VnpayGateway;
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
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    final OrderRepository orders = mock(OrderRepository.class);
    final CartRepository carts = mock(CartRepository.class);
    final OrderActorResolver actors = mock(OrderActorResolver.class);
    final VnpayGateway gateway = mock(VnpayGateway.class);
    final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    final Instant now = Instant.parse("2026-10-28T08:00:00Z");
    final UUID actor = UUID.randomUUID(), product = UUID.randomUUID(), id = UUID.randomUUID(), quoteId = UUID.randomUUID();
    final Line line = new Line(product, "PC", "PC-01", BigDecimal.valueOf(100_000), 2, 36);
    final CartSession cart = new CartSession(UUID.randomUUID(), 3, Map.of(product, 2));
    final OrderService service = new OrderService(orders, carts, actors, gateway, events, Clock.fixed(now, ZoneOffset.UTC));
    @BeforeEach void setup() {
        when(actors.require(eq(actor), anyString())).thenReturn(new Account(actor, "Customer", "demo@example.test", "+84912345678", true, List.of("CUSTOMER", "ADMIN")));
        when(carts.lock(actor)).thenReturn(cart);
        when(orders.lockProduct(product, 2)).thenReturn(new OrderRepository.Stock(line, 5, "ACTIVE"));
        when(orders.lockProduct(product, 3)).thenReturn(new OrderRepository.Stock(line, 5, "ACTIVE"));
        when(orders.lockQuote(quoteId, actor)).thenReturn(quote(now.plusSeconds(600)));
        when(orders.replay(eq(actor), anyString(), any())).thenReturn(Optional.empty());
        when(orders.get(any(), anyBoolean())).thenReturn(order(OrderStatus.PENDING_CONFIRMATION, PaymentMethod.COD, "UNPAID", "HELD", now.plusSeconds(600)));
    }
    Quote quote(Instant expiry) { return new Quote(quoteId, actor, 3, "Customer", "+84912345678", "123 Street, Ward, City", expiry, List.of(line), OrderService.SHIPPING_FEE); }
    Order order(OrderStatus status, PaymentMethod method, String payment, String reservation, Instant expiry) {
        return new Order(id, "PC-01", actor, "demo@example.test", "Customer", "+84912345678", "Address", method,
                status, payment, reservation, 4, expiry, now, OrderService.SHIPPING_FEE, BigDecimal.valueOf(250_000), "ref", null, List.of(line), List.of());
    }
    @Test void quoteUsesServerPriceAndDoesNotReserve() {
        Quote result = service.quote(actor, new QuoteRequest(3, new Shipping("Customer", "0912345678", "City", "Ward", "123 Street")));
        assertEquals(now.plusSeconds(600), result.expiresAt());
        assertEquals(BigDecimal.valueOf(250_000), result.total());
        assertEquals("+84912345678", result.phone());
        verify(orders, never()).reserve(any(), anyInt());
    }
    @Test void adminAccountRequiresAdminRole() {
        assertEquals(actor, service.adminAccount(actor).id());
        verify(actors).require(actor, "ADMIN");
    }
    @Test void staleCartCannotQuote() {
        assertEquals("CART_VERSION_CONFLICT", assertThrows(OrderException.class, () -> service.quote(actor,
                new QuoteRequest(2, new Shipping("Customer", "0912345678", "City", "Ward", "123 Street")))).code());
        verify(orders, never()).saveQuote(any());
    }
    @Test void unverifiedPhoneCannotCheckout() {
        when(actors.require(actor, "CUSTOMER")).thenReturn(new Account(actor, "Customer", "mail", "+84912345678", false, List.of("CUSTOMER")));
        assertEquals("PHONE_NOT_VERIFIED", assertThrows(OrderException.class, () -> service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD), "127.0.0.1")).code());
        verify(orders, never()).reserve(any(), anyInt());
    }
    @Test void expiredQuoteCannotReserve() {
        when(orders.lockQuote(quoteId, actor)).thenReturn(quote(now));
        assertEquals("QUOTE_EXPIRED", assertThrows(OrderException.class, () -> service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD), "127.0.0.1")).code());
        verify(orders, never()).reserve(any(), anyInt());
    }
    @Test void replayDoesNotCreateOrReserveAgain() {
        when(orders.replay(eq(actor), anyString(), any())).thenReturn(Optional.of(id));
        assertEquals(id, service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD), "127.0.0.1").id());
        verify(orders, never()).lockQuote(any(), any()); verify(orders, never()).reserve(any(), anyInt());
        verify(carts, never()).save(any());
    }
    @Test void placementReservesAndClearsCartAndKeepsQuotePrice() {
        // A catalog price change during quote validity must not change the accepted price snapshot.
        when(orders.lockProduct(product, 2)).thenReturn(new OrderRepository.Stock(new Line(product, "New name", "PC-01", BigDecimal.valueOf(150_000), 2, 36), 5, "ACTIVE"));
        service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD), "127.0.0.1");
        verify(orders).reserve(product, 2);
        verify(orders).create(any(), argThat(q -> q.total().equals(BigDecimal.valueOf(250_000))), anyString(), eq(PaymentMethod.COD), eq(now.plus(Duration.ofHours(24))), isNull());
        assertTrue(cart.quantities().isEmpty()); assertEquals(4, cart.version());
        verify(carts).save(cart); verify(events).publishEvent(any(Object.class));
    }
    @Test void insufficientStockDoesNotCreateOrder() {
        when(orders.lockProduct(product, 2)).thenReturn(new OrderRepository.Stock(line, 1, "ACTIVE"));
        assertThrows(OrderException.class, () -> service.place(actor, "request-123", new PlaceRequest(quoteId, PaymentMethod.COD), "127.0.0.1"));
        verify(orders, never()).create(any(), any(), any(), any(), any(), any());
    }
    @Test void cancellationReleasesReservation() {
        service.transition(actor, id, false, new TransitionRequest(4, OrderStatus.CANCELLED, "Cancel"));
        verify(orders).reserve(product, -2); verify(orders).state(any(), eq(OrderStatus.CANCELLED), eq("RELEASED"));
    }
    @Test void shippingConsumesStockAndCodOnlyPaidOnCompletion() {
        when(orders.get(id, true)).thenReturn(order(OrderStatus.CONFIRMED, PaymentMethod.COD, "UNPAID", "HELD", now.minusSeconds(1)));
        service.transition(actor, id, true, new TransitionRequest(4, OrderStatus.SHIPPING, "Ship"));
        verify(orders).consume(product, 2); verify(orders, never()).payment(any(), eq("PAID"), any());
        when(orders.get(id, true)).thenReturn(order(OrderStatus.SHIPPING, PaymentMethod.COD, "UNPAID", "CONSUMED", now.minusSeconds(1)));
        service.transition(actor, id, true, new TransitionRequest(4, OrderStatus.COMPLETED, "Delivered"));
        verify(orders).payment(any(), eq("PAID"), anyString());
    }
    @Test void paidVnpayOrderNeverExpiresWhileWaitingForConfirmation() {
        when(orders.get(id, true)).thenReturn(order(OrderStatus.PENDING_CONFIRMATION, PaymentMethod.VNPAY, "PAID", "HELD", now.minusSeconds(1)));
        service.expire(id);
        verify(orders, never()).reserve(any(), anyInt()); verify(orders, never()).payment(any(), any(), any());
        service.transition(actor, id, true, new TransitionRequest(4, OrderStatus.CONFIRMED, "Confirm"));
        verify(orders).state(any(), eq(OrderStatus.CONFIRMED), eq("HELD"));
    }
    @Test void expiryReleasesUnpaidOrder() {
        when(orders.get(id, true)).thenReturn(order(OrderStatus.PENDING_PAYMENT, PaymentMethod.VNPAY, "UNPAID", "HELD", now));
        service.expire(id);
        verify(orders).reserve(product, -2); verify(orders).state(any(), eq(OrderStatus.EXPIRED), eq("RELEASED"));
    }
    @Test void customerCannotReadAnotherCustomersOrder() {
        UUID stranger = UUID.randomUUID();
        assertEquals(404, assertThrows(OrderException.class, () -> service.get(stranger, id, false)).status().value());
    }
    @Test void staleOrderCannotMutate() {
        assertEquals("ORDER_VERSION_CONFLICT", assertThrows(OrderException.class, () -> service.transition(actor, id, true,
                new TransitionRequest(3, OrderStatus.CONFIRMED, "Confirm"))).code());
        verify(orders, never()).state(any(), any(), any());
    }
    @Test void codQuantityChangeAdjustsReservationAndTotalAtSnapshotPrice() {
        service.quantity(actor, id, product, new QuantityRequest(4, 3, "Customer requested"));
        verify(orders).reserve(product, 1); verify(orders).quantity(any(), eq(product), eq(3), eq(BigDecimal.valueOf(350_000)));
    }
    @Test void customerCannotConfirmAndCompletedOrderCannotCancel() {
        assertThrows(OrderException.class, () -> OrderStatus.PENDING_CONFIRMATION.requireTransition(OrderStatus.CONFIRMED, false));
        assertThrows(OrderException.class, () -> OrderStatus.COMPLETED.requireTransition(OrderStatus.CANCELLED, true));
    }
}
