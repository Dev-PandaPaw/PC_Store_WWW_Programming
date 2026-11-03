package iuh.fit.nhom16.pcstore.sales.service;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.repository.OrderRepository;
import iuh.fit.nhom16.pcstore.sales.security.OrderActorResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class OrderService {
    public static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(50_000);
    private final OrderRepository orders;
    private final CartRepository carts;
    private final OrderActorResolver actors;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    public OrderService(OrderRepository orders, CartRepository carts, OrderActorResolver actors,
                        ApplicationEventPublisher events, Clock clock) {
        this.orders = orders; this.carts = carts; this.actors = actors; this.events = events; this.clock = clock;
    }
    public Account account(UUID actor) { return actors.require(actor, "CUSTOMER"); }
    public Account adminAccount(UUID actor) { return actors.require(actor, "ADMIN"); }
    public Quote quote(UUID actor, QuoteRequest request) {
        Account customer = actors.require(actor, "CUSTOMER");
        requirePhone(customer, request.shipping().normalizedPhone());
        CartSession cart = carts.lock(actor);
        requireCart(cart, request.cartVersion());
        List<Line> lines = new ArrayList<>();
        cart.quantities().entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString())).forEach(entry -> {
            if (entry.getValue() < 1 || entry.getValue() > 99) throw OrderException.conflict("INVALID_QUANTITY", "Số lượng phải từ 1 đến 99.");
            var stock = orders.lockProduct(entry.getKey(), entry.getValue());
            requireStock(stock, entry.getValue());
            lines.add(stock.line());
        });
        Quote quote = new Quote(UUID.randomUUID(), actor, cart.version(), request.shipping().recipient().trim(),
                request.shipping().normalizedPhone(), request.shipping().address(), clock.instant().plus(Duration.ofMinutes(10)),
                List.copyOf(lines), SHIPPING_FEE);
        orders.saveQuote(quote);
        return quote;
    }
    public Order place(UUID actor, String key, PlaceRequest request) {
        if (request.paymentMethod() != PaymentMethod.COD) throw new OrderException(HttpStatus.BAD_REQUEST,
                "PAYMENT_METHOD_UNSUPPORTED", "Đặt hàng chỉ hỗ trợ thanh toán khi nhận hàng (COD).");
        if (key == null || !key.matches("[a-zA-Z0-9_-]{8,100}")) throw new OrderException(HttpStatus.BAD_REQUEST,
                "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key phải dài 8–100 ký tự chữ, số, _ hoặc -.");
        Account customer = actors.require(actor, "CUSTOMER");
        // Every checkout for the same customer locks the same row, including retries on another instance.
        CartSession cart = carts.lock(actor);
        var replay = orders.replay(actor, key, request);
        if (replay.isPresent()) return orders.get(replay.get(), true);
        Quote quote = orders.lockQuote(request.quoteId(), actor);
        Instant now = clock.instant();
        if (!now.isBefore(quote.expiresAt())) throw OrderException.conflict("QUOTE_EXPIRED", "Bản xem trước đã hết hạn. Hãy tạo lại.");
        requirePhone(customer, quote.phone());
        requireCart(cart, quote.cartVersion());
        Map<UUID, Integer> quantities = new HashMap<>();
        for (Line line : quote.items()) quantities.put(line.productId(), line.quantity());
        if (!cart.quantities().equals(quantities)) throw OrderException.conflict("CART_CHANGED", "Giỏ hàng đã thay đổi.");
        for (Line line : sorted(quote.items())) {
            requireStock(orders.lockProduct(line.productId(), line.quantity()), line.quantity());
            orders.reserve(line.productId(), line.quantity());
        }
        UUID id = UUID.randomUUID();
        Instant expiry = now.plus(Duration.ofHours(24));
        orders.create(id, quote, customer.email(), expiry);
        orders.history(id, null, "PENDING_CONFIRMATION", actor, "Tạo đơn hàng");
        orders.remember(actor, key, request, id);
        cart.quantities().clear(); cart.incrementVersion(); carts.save(cart);
        Order order = orders.get(id, false);
        events.publishEvent(new OrderNotification(order.email(), order.code(), order.status().name()));
        return order;
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<Order> list(UUID actor, boolean admin, int page) {
        actors.require(actor, admin ? "ADMIN" : "CUSTOMER");
        if (page < 0 || page > 100_000) throw new IllegalArgumentException("Trang không hợp lệ.");
        return orders.list(admin ? null : actor, page).stream().map(id -> orders.get(id, false)).toList();
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Order get(UUID actor, UUID id, boolean admin) {
        actors.require(actor, admin ? "ADMIN" : "CUSTOMER");
        return owned(actor, id, admin, false);
    }
    public Order transition(UUID actor, UUID id, boolean admin, TransitionRequest request) {
        actors.require(actor, admin ? "ADMIN" : "CUSTOMER");
        Order order = owned(actor, id, admin, true);
        requireVersion(order, request.version());
        requireNotExpired(order);
        order.status().requireTransition(request.status(), admin);
        if (request.status() == OrderStatus.CANCELLED && "PAID".equals(order.paymentStatus()))
            throw OrderException.conflict("PAID_ORDER_CANNOT_CANCEL", "Đơn đã thanh toán cần quy trình hoàn tiền.");
        String reservation = order.reservationStatus();
        if (request.status() == OrderStatus.CANCELLED) {
            release(order); reservation = "RELEASED";
            orders.paymentStatus(order, "CANCELLED");
        } else if (request.status() == OrderStatus.SHIPPING) {
            for (Line line : sorted(order.items())) { orders.lockProduct(line.productId(), line.quantity()); orders.consume(line.productId(), line.quantity()); }
            reservation = "CONSUMED";
        } else if (request.status() == OrderStatus.COMPLETED && order.paymentMethod() == PaymentMethod.COD) {
            orders.paymentStatus(order, "PAID");
        }
        orders.state(order, request.status(), reservation);
        orders.history(id, order.status().name(), request.status().name(), actor, request.reason());
        events.publishEvent(new OrderNotification(order.email(), order.code(), request.status().name()));
        return orders.get(id, false);
    }
    public Order quantity(UUID actor, UUID id, UUID product, QuantityRequest request) {
        actors.require(actor, "ADMIN");
        Order order = orders.get(id, true);
        requireVersion(order, request.version()); requireNotExpired(order);
        if (order.paymentMethod() != PaymentMethod.COD || order.status() != OrderStatus.PENDING_CONFIRMATION || !"UNPAID".equals(order.paymentStatus()))
            throw OrderException.conflict("COD_EDIT_FORBIDDEN", "Chỉ sửa lượng COD chưa thanh toán, chờ xác nhận.");
        Line line = order.items().stream().filter(item -> item.productId().equals(product)).findFirst().orElseThrow(OrderException::missing);
        int delta = request.quantity() - line.quantity();
        var stock = orders.lockProduct(product, request.quantity());
        if (delta > 0) requireStock(stock, delta);
        orders.reserve(product, delta);
        BigDecimal total = order.total().add(line.unitPrice().multiply(BigDecimal.valueOf(delta)));
        orders.quantity(order, product, request.quantity(), total);
        orders.history(id, order.status().name(), order.status().name(), actor,
                "COD " + product + ": " + line.quantity() + " -> " + request.quantity() + ". " + request.reason());
        return orders.get(id, false);
    }
    public void expire(UUID id) {
        Order order = orders.get(id, true);
        if ("UNPAID".equals(order.paymentStatus()) && (order.status() == OrderStatus.PENDING_PAYMENT || order.status() == OrderStatus.PENDING_CONFIRMATION)
                && !clock.instant().isBefore(order.expiresAt())) {
            release(order);
            orders.state(order, OrderStatus.EXPIRED, "RELEASED");
            orders.paymentStatus(order, "EXPIRED");
            orders.history(id, order.status().name(), "EXPIRED", null, "Hết thời gian chờ");
            events.publishEvent(new OrderNotification(order.email(), order.code(), "EXPIRED"));
        }
    }
    private Order owned(UUID actor, UUID id, boolean admin, boolean lock) {
        Order order = orders.get(id, lock);
        if (!admin && !actor.equals(order.customerId())) throw OrderException.missing();
        return order;
    }
    private void release(Order order) {
        if (!"HELD".equals(order.reservationStatus())) throw OrderException.conflict("RESERVATION_CONFLICT", "Đơn không còn giữ hàng.");
        for (Line line : sorted(order.items())) { orders.lockProduct(line.productId(), line.quantity()); orders.reserve(line.productId(), -line.quantity()); }
    }
    private List<Line> sorted(List<Line> lines) { return lines.stream().sorted(Comparator.comparing(line -> line.productId().toString())).toList(); }
    private void requireCart(CartSession cart, long version) {
        if (cart.version() != version) throw OrderException.conflict("CART_VERSION_CONFLICT", "Giỏ hàng đã thay đổi. Hãy tải lại.");
        if (cart.quantities().isEmpty() || cart.quantities().size() > 100) throw OrderException.conflict("INVALID_CART", "Giỏ phải có từ 1 đến 100 sản phẩm.");
    }
    private void requirePhone(Account customer, String phone) {
        String verified = customer.phone() == null ? "" : customer.phone().replaceFirst("^0", "+84");
        if (!customer.phoneVerified() || !verified.equals(phone)) throw OrderException.conflict("PHONE_NOT_VERIFIED", "Số điện thoại nhận hàng phải là số đã xác minh của tài khoản.");
    }
    private void requireStock(OrderRepository.Stock stock, int quantity) {
        if (!"ACTIVE".equals(stock.status()) || stock.available() < quantity || stock.line().unitPrice().signum() <= 0
                || stock.line().unitPrice().stripTrailingZeros().scale() > 0)
            throw OrderException.conflict("INSUFFICIENT_STOCK", "Sản phẩm ngừng bán hoặc không đủ tồn kho.");
    }
    private void requireVersion(Order order, long version) {
        if (order.version() != version) throw OrderException.conflict("ORDER_VERSION_CONFLICT", "Đơn đã được cập nhật. Hãy tải lại.");
    }
    private void requireNotExpired(Order order) {
        if ("UNPAID".equals(order.paymentStatus()) && (order.status() == OrderStatus.PENDING_PAYMENT || order.status() == OrderStatus.PENDING_CONFIRMATION)
                && !clock.instant().isBefore(order.expiresAt())) throw OrderException.conflict("ORDER_EXPIRED", "Đơn đã hết hạn chờ.");
    }
}
