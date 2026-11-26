package iuh.fit.nhom16.pcstore.sales.service;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.pcbuild.PcBuildCheckoutGuard;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import iuh.fit.nhom16.pcstore.sales.repository.OrderRepository;
import iuh.fit.nhom16.pcstore.sales.security.OrderActorResolver;
import iuh.fit.nhom16.pcstore.sales.service.ShippingPolicy.ResolvedShipping;
import iuh.fit.nhom16.pcstore.sales.service.VietnamAddressRegistry.VerifiedLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    public static final BigDecimal SHIPPING_FEE = ShippingPolicy.EXPRESS_FEE;

    private final OrderRepository orders;
    private final CartRepository carts;
    private final OrderActorResolver actors;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final PcBuildCheckoutGuard buildCheckout;

    public OrderService(OrderRepository orders, CartRepository carts, OrderActorResolver actors,
                        ApplicationEventPublisher events, Clock clock, PcBuildCheckoutGuard buildCheckout) {
        this.orders = orders;
        this.carts = carts;
        this.actors = actors;
        this.events = events;
        this.clock = clock;
        this.buildCheckout = buildCheckout;
    }

    public Account account(UUID actor) {
        return actors.require(actor, "CUSTOMER");
    }

    public Account adminAccount(UUID actor) {
        return actors.require(actor, "ADMIN");
    }

    @Transactional(readOnly = true)
    public List<ShippingOptionView> shippingOptions(UUID actor, String province, String district) {
        actors.require(actor, "CUSTOMER");
        return ShippingPolicy.optionsForLocation(province, district);
    }

    @Transactional(readOnly = true)
    public List<VoucherView> availableVouchers(UUID actor) {
        actors.require(actor, "CUSTOMER");
        return orders.activeVouchers(clock.instant());
    }

    @Transactional(readOnly = true)
    public Optional<Order> checkIdempotencyResult(UUID actor, String key) {
        actors.require(actor, "CUSTOMER");
        validateIdempotencyKey(key);
        return orders.findOrderByIdempotencyKey(actor, key).map(orderId -> orders.get(orderId, false));
    }

    public Quote quote(UUID actor, QuoteRequest request) {
        Account customer = actors.require(actor, "CUSTOMER");

        ResolvedShipping resolvedShipping = ShippingPolicy.verifyAndResolve(
                request.resolvedShippingMethod(), request.shipping());
        VerifiedLocation verifiedLocation = resolvedShipping.location();
        ShippingMethod shippingMethod = resolvedShipping.method();
        BigDecimal shippingFee = resolvedShipping.fee();
        PaymentMethod paymentMethod = request.resolvedPaymentMethod();

        CartSession cart = carts.lock(actor);
        requireValidCartVersion(cart, request.cartVersion());

        Map<UUID, Integer> selectedItems = resolveSelectedCartItems(cart, request.selectedProductIds());
        List<Line> lines = new ArrayList<>();
        List<Map<String, Object>> itemIssues = new ArrayList<>();
        for (var entry : selectedItems.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().toString())).toList()) {
            int qty = entry.getValue();
            if (qty < 1 || qty > 99) {
                throw OrderException.conflict("INVALID_QUANTITY", "Số lượng đặt mua phải là số nguyên từ 1 đến 99.");
            }
            OrderRepository.Stock stock;
            try {
                stock = orders.lockProduct(entry.getKey(), qty);
            } catch (OrderException missingEx) {
                itemIssues.add(Map.of(
                        "productId", entry.getKey(),
                        "requestedQuantity", qty,
                        "availableQuantity", 0,
                        "reason", "PRODUCT_DELETED"));
                continue;
            }
            Optional<Map<String, Object>> issue = inspectProductIssue(stock, qty);
            if (issue.isPresent()) {
                itemIssues.add(issue.get());
            } else {
                lines.add(stock.line());
            }
        }
        throwIfAnyProductIssues(itemIssues);

        if (selectedItems.keySet().equals(cart.quantities().keySet())) {
            buildCheckout.requireReady(cart, lines);
        }

        BigDecimal subtotal = sumSubtotal(lines);
        BigDecimal taxAmount = BigDecimal.ZERO;
        String voucherCode = request.normalizedVoucherCode();
        VoucherRuleSnapshot voucherRule = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucherCode != null) {
            OrderRepository.Voucher voucher = orders.findVoucher(voucherCode)
                    .orElseThrow(() -> OrderException.conflict("VOUCHER_INVALID", "Mã giảm giá không tồn tại hoặc không hợp lệ."));
            validateVoucherStrict(voucher, actor, subtotal, clock.instant());
            voucherRule = voucher.toRuleSnapshot();
            discountAmount = voucherRule.calculateDiscount(subtotal);
        }

        PricingBreakdown pricing = PricingBreakdown.of(subtotal, shippingFee, taxAmount, voucherCode, discountAmount);
        Quote quote = new Quote(
                UUID.randomUUID(),
                actor,
                cart.version(),
                request.shipping().recipient().trim(),
                request.shipping().normalizedPhone(),
                verifiedLocation.formattedAddress(),
                verifiedLocation.provinceCode(),
                verifiedLocation.districtCode(),
                verifiedLocation.wardCode(),
                shippingMethod,
                paymentMethod,
                clock.instant().plus(Duration.ofMinutes(10)),
                List.copyOf(lines),
                pricing.subtotal(),
                pricing.shippingFee(),
                pricing.taxAmount(),
                pricing.voucherCode(),
                pricing.voucherCode() == null ? null : voucherRule,
                pricing.discountAmount(),
                pricing.total());

        orders.saveQuote(quote);
        log.info("Order quote created: quoteId={}, customerId={}, items={}, provinceCode={}, districtCode={}, shippingMethod={}, voucher={}, subtotal={}, discount={}, shipping={}, total={}",
                quote.id(), actor, lines.size(), verifiedLocation.provinceCode(), verifiedLocation.districtCode(),
                shippingMethod, pricing.voucherCode(), pricing.subtotal(), pricing.discountAmount(), pricing.shippingFee(), pricing.total());
        return quote;
    }

    public Order place(UUID actor, String key, PlaceRequest request) {
        if (request.paymentMethod() == null) {
            throw OrderException.badRequest("PAYMENT_METHOD_REQUIRED", "Vui lòng chọn phương thức thanh toán.");
        }
        validateIdempotencyKey(key);

        Account customer = actors.require(actor, "CUSTOMER");
        // Lock the customer's cart row first to serialize concurrent placement/retries across tabs/instances.
        CartSession cart = carts.lock(actor);

        Optional<UUID> replay = orders.replay(actor, key, request);
        if (replay.isPresent()) {
            log.info("Order placement idempotent replay: customerId={}, key={}, orderId={}", actor, key, replay.get());
            return orders.get(replay.get(), true);
        }

        Quote quote = orders.lockQuote(request.quoteId(), actor);
        Instant now = clock.instant();
        if (!now.isBefore(quote.expiresAt())) {
            throw OrderException.conflict("QUOTE_EXPIRED", "Bản xác nhận đơn hàng đã hết hạn. Vui lòng kiểm tra và xác nhận lại.");
        }
        if (quote.items() == null || quote.items().isEmpty()) {
            throw OrderException.conflict("EMPTY_ORDER_ITEMS", "Đơn hàng không có sản phẩm nào được chọn.");
        }

        // Verify all selected items in the quote still exist in the customer's cart with sufficient quantity,
        // WITHOUT rejecting or wiping unrelated items/quantities added in another tab!
        for (Line line : quote.items()) {
            Integer inCart = cart.quantities().get(line.productId());
            if (inCart == null || inCart < line.quantity()) {
                throw OrderException.conflict(
                        "CART_SELECTION_CHANGED",
                        "Sản phẩm \"" + line.name() + "\" trong giỏ hàng đã thay đổi hoặc bị xóa ở phiên khác. Vui lòng xác nhận lại đơn hàng.",
                        Map.of("reconfirmationRequired", true, "productId", line.productId()));
            }
        }

        // Revalidate every product in deterministic UUID order under FOR UPDATE lock
        List<Line> sortedLines = sorted(quote.items());
        List<Line> latestLines = new ArrayList<>();
        List<Map<String, Object>> itemIssues = new ArrayList<>();
        List<Map<String, Object>> priceChanges = new ArrayList<>();
        for (Line quotedLine : sortedLines) {
            OrderRepository.Stock liveStock;
            try {
                liveStock = orders.lockProduct(quotedLine.productId(), quotedLine.quantity());
            } catch (OrderException missingEx) {
                itemIssues.add(Map.of(
                        "productId", quotedLine.productId(),
                        "name", quotedLine.name(),
                        "requestedQuantity", quotedLine.quantity(),
                        "availableQuantity", 0,
                        "reason", "PRODUCT_DELETED"));
                continue;
            }
            Optional<Map<String, Object>> issue = inspectProductIssue(liveStock, quotedLine.quantity());
            if (issue.isPresent()) {
                itemIssues.add(issue.get());
                continue;
            }
            latestLines.add(liveStock.line());
            if (liveStock.line().unitPrice().compareTo(quotedLine.unitPrice()) != 0) {
                priceChanges.add(Map.of(
                        "productId", quotedLine.productId(),
                        "name", liveStock.line().name(),
                        "oldPrice", quotedLine.unitPrice(),
                        "newPrice", liveStock.line().unitPrice(),
                        "quantity", quotedLine.quantity()));
            }
        }
        throwIfAnyProductIssues(itemIssues);

        BigDecimal liveSubtotal = sumSubtotal(latestLines);
        BigDecimal liveTax = BigDecimal.ZERO;
        BigDecimal liveShippingFee = ShippingPolicy.resolveFeeByCodes(
                quote.shippingMethod(), quote.provinceCode(), quote.districtCode(), null, null);

        // Lock & revalidate voucher against liveSubtotal so ANY recalculation (including price/shipping change)
        // uses accurate, freshly computed voucher eligibility and discount!
        OrderRepository.Voucher lockedVoucher = null;
        String liveVoucherCode = null;
        VoucherRuleSnapshot liveVoucherRule = null;
        BigDecimal liveDiscount = BigDecimal.ZERO;
        Optional<String> voucherInvalidReason = Optional.empty();

        if (quote.voucherCode() != null && !quote.voucherCode().isBlank()) {
            Optional<OrderRepository.Voucher> voucherOpt = orders.lockVoucher(quote.voucherCode());
            if (voucherOpt.isEmpty()) {
                voucherInvalidReason = Optional.of("Mã giảm giá \"" + quote.voucherCode() + "\" không còn tồn tại.");
            } else {
                voucherInvalidReason = checkVoucherInvalidReason(voucherOpt.get(), actor, liveSubtotal, now);
                if (voucherInvalidReason.isEmpty()) {
                    lockedVoucher = voucherOpt.get();
                    liveVoucherCode = lockedVoucher.code();
                    liveVoucherRule = lockedVoucher.toRuleSnapshot();
                    liveDiscount = liveVoucherRule.calculateDiscount(liveSubtotal);
                }
            }
        }

        PricingBreakdown livePricing = PricingBreakdown.of(
                liveSubtotal, liveShippingFee, liveTax, liveVoucherCode, liveDiscount);

        if (!priceChanges.isEmpty()) {
            log.warn("Order placement rejected due to price change: customerId={}, quoteId={}, changes={}",
                    actor, quote.id(), priceChanges.size());
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("reconfirmationRequired", true);
            details.put("priceChanges", priceChanges);
            details.put("recalculatedPricing", livePricing);
            if (voucherInvalidReason.isPresent()) {
                details.put("invalidVoucherCode", quote.voucherCode());
            }
            throw OrderException.conflict(
                    "PRICE_CHANGED",
                    "Giá một số sản phẩm vừa thay đổi. Hệ thống đã tính lại tổng tiền (bao gồm điều kiện voucher), vui lòng kiểm tra và xác nhận lại.",
                    details);
        }

        if (quote.shippingFee() != null && livePricing.shippingFee().compareTo(quote.shippingFee()) != 0) {
            throw OrderException.conflict(
                    "SHIPPING_FEE_CHANGED",
                    "Phí vận chuyển đã thay đổi (" + quote.shippingFee() + "đ -> " + livePricing.shippingFee() + "đ). Vui lòng xác nhận lại tổng tiền.",
                    Map.of(
                            "reconfirmationRequired", true,
                            "oldShippingFee", quote.shippingFee(),
                            "newShippingFee", livePricing.shippingFee(),
                            "recalculatedPricing", livePricing));
        }

        if (voucherInvalidReason.isPresent()) {
            log.warn("Order placement voucher invalid at confirmation: customerId={}, quoteId={}, voucher={}, reason={}",
                    actor, quote.id(), quote.voucherCode(), voucherInvalidReason.get());
            throw OrderException.conflict(
                    "VOUCHER_RECONFIRMATION_REQUIRED",
                    voucherInvalidReason.get() + " Tổng tiền đã được tính lại không áp dụng mã giảm giá, vui lòng xác nhận lại.",
                    Map.of(
                            "reconfirmationRequired", true,
                            "invalidVoucherCode", quote.voucherCode(),
                            "recalculatedPricing", livePricing));
        }

        BigDecimal quotedDiscount = quote.discountAmount() == null ? BigDecimal.ZERO : quote.discountAmount();
        if (livePricing.discountAmount().compareTo(quotedDiscount) != 0
                || livePricing.total().compareTo(quote.total()) != 0) {
            throw OrderException.conflict(
                    "VOUCHER_RECONFIRMATION_REQUIRED",
                    "Tổng tiền hoặc mức giảm giá đã thay đổi. Vui lòng kiểm tra tổng tiền mới và xác nhận lại.",
                    Map.of("reconfirmationRequired", true, "recalculatedPricing", livePricing));
        }

        Set<UUID> quotedProductIds = new LinkedHashSet<>();
        quote.items().forEach(item -> quotedProductIds.add(item.productId()));
        if (quotedProductIds.equals(cart.quantities().keySet())) {
            buildCheckout.requireReady(cart, quote.items());
        }

        // Atomic inventory reservation across all ordered lines inside the single transaction
        for (Line line : sortedLines) {
            orders.reserve(line.productId(), line.quantity());
        }

        Quote verifiedQuoteToPersist = new Quote(
                quote.id(),
                quote.customerId(),
                quote.cartVersion(),
                quote.recipient(),
                quote.phone(),
                quote.address(),
                quote.provinceCode(),
                quote.districtCode(),
                quote.wardCode(),
                quote.shippingMethod(),
                request.paymentMethod(),
                quote.expiresAt(),
                quote.items(),
                livePricing.subtotal(),
                livePricing.shippingFee(),
                livePricing.taxAmount(),
                livePricing.voucherCode(),
                livePricing.voucherCode() == null ? null : liveVoucherRule,
                livePricing.discountAmount(),
                livePricing.total());

        UUID orderId = UUID.randomUUID();
        OrderStatus initialStatus = request.paymentMethod() == PaymentMethod.COD
                ? OrderStatus.PENDING_CONFIRMATION
                : OrderStatus.PENDING_PAYMENT;
        Instant expiry = request.paymentMethod() == PaymentMethod.COD
                ? now.plus(Duration.ofHours(24))
                : now.plus(Duration.ofMinutes(30));

        orders.create(orderId, verifiedQuoteToPersist, request.paymentMethod(), initialStatus, customer.email(), expiry);

        if (lockedVoucher != null && livePricing.voucherCode() != null) {
            orders.consumeVoucher(lockedVoucher.code(), lockedVoucher.version(), orderId, actor, livePricing.discountAmount());
        }

        orders.history(orderId, null, initialStatus.name(), actor, "Tạo đơn hàng");
        orders.remember(actor, key, request, orderId);

        // Deduct ONLY the purchased items & quantities from the cart, preserving unselected items
        // and any extra quantities or new products added in another browser tab!
        deductOrderedItemsFromCart(cart, quote.items());
        carts.save(cart);

        Order order = orders.get(orderId, false);
        events.publishEvent(new OrderNotification(order.email(), order.code(), order.status().name()));
        log.info("Order created successfully: orderId={}, code={}, customerId={}, status={}, paymentMethod={}, subtotal={}, discount={}, shipping={}, total={}",
                order.id(), order.code(), actor, order.status(), order.paymentMethod(),
                order.subtotal(), order.discountAmount(), order.shippingFee(), order.total());
        return order;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Order> list(UUID actor, boolean admin, int page) {
        actors.require(actor, admin ? "ADMIN" : "CUSTOMER");
        if (page < 0 || page > 100_000) throw new IllegalArgumentException("Trang không hợp lệ.");
        return orders.list(admin ? null : actor, page).stream().map(id -> orders.get(id, false)).toList();
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
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

        if (request.status() == OrderStatus.CANCELLED && "PAID".equals(order.paymentStatus())) {
            throw OrderException.conflict("PAID_ORDER_CANNOT_CANCEL", "Đơn đã thanh toán cần quy trình hoàn tiền.");
        }

        String reservation = order.reservationStatus();
        if (request.status() == OrderStatus.CANCELLED) {
            release(order);
            orders.releaseVoucher(id);
            reservation = "RELEASED";
            orders.paymentStatus(order, "CANCELLED");
        } else if (request.status() == OrderStatus.SHIPPING) {
            for (Line line : sorted(order.items())) {
                orders.lockProduct(line.productId(), line.quantity());
                orders.consume(line.productId(), line.quantity());
            }
            reservation = "CONSUMED";
        } else if ((request.status() == OrderStatus.DELIVERED || request.status() == OrderStatus.COMPLETED)
                && order.paymentMethod() == PaymentMethod.COD
                && !"PAID".equals(order.paymentStatus())) {
            orders.paymentStatus(order, "PAID");
        }

        orders.state(order, request.status(), reservation);
        orders.history(id, order.status().name(), request.status().name(), actor, request.reason());
        events.publishEvent(new OrderNotification(order.email(), order.code(), request.status().name()));
        log.info("Order state transitioned: orderId={}, actorId={}, from={}, to={}",
                id, actor, order.status(), request.status());
        return orders.get(id, false);
    }

    /**
     * Adjusts quantity on an unpaid COD order waiting for confirmation AND recalculates subtotal,
     * voucher eligibility (removing voucher & releasing usage if newSubtotal drops below minOrderSubtotal),
     * percentage/capped discount amount, and final total using BigDecimal.
     */
    public Order quantity(UUID actor, UUID id, UUID product, QuantityRequest request) {
        actors.require(actor, "ADMIN");
        Order order = orders.get(id, true);
        requireVersion(order, request.version());
        requireNotExpired(order);
        if (order.paymentMethod() != PaymentMethod.COD
                || order.status() != OrderStatus.PENDING_CONFIRMATION
                || !"UNPAID".equals(order.paymentStatus())) {
            throw OrderException.conflict("COD_EDIT_FORBIDDEN", "Chỉ sửa lượng COD chưa thanh toán, chờ xác nhận.");
        }

        Line existingLine = order.items().stream()
                .filter(item -> item.productId().equals(product))
                .findFirst()
                .orElseThrow(OrderException::missing);
        int delta = request.quantity() - existingLine.quantity();
        OrderRepository.Stock stock = orders.lockProduct(product, request.quantity());
        if (delta > 0) {
            Optional<Map<String, Object>> issue = inspectProductIssue(stock, delta);
            issue.ifPresent(i -> throwIfAnyProductIssues(List.of(i)));
        }
        orders.reserve(product, delta);

        List<Line> updatedLines = order.items().stream()
                .map(item -> item.productId().equals(product)
                        ? new Line(item.productId(), item.name(), item.sku(), item.unitPrice(), request.quantity(), item.warrantyMonths())
                        : item)
                .toList();
        BigDecimal newSubtotal = sumSubtotal(updatedLines);
        BigDecimal shippingFee = order.shippingFee() == null ? BigDecimal.ZERO : order.shippingFee();
        BigDecimal taxAmount = order.taxAmount() == null ? BigDecimal.ZERO : order.taxAmount();

        String updatedVoucherCode = order.voucherCode();
        VoucherRuleSnapshot updatedRule = resolveOrderVoucherRule(order);
        BigDecimal updatedDiscount = BigDecimal.ZERO;
        String voucherAuditNote = "";

        if (updatedVoucherCode != null && !updatedVoucherCode.isBlank()) {
            if (updatedRule == null || !updatedRule.isEligible(newSubtotal)) {
                // Subtotal dropped below voucher's minimum order requirement -> remove voucher & release usage slot
                String removedCode = updatedVoucherCode;
                updatedVoucherCode = null;
                updatedRule = null;
                updatedDiscount = BigDecimal.ZERO;
                orders.releaseVoucher(order.id());
                voucherAuditNote = " (Đã gỡ mã giảm giá " + removedCode + " do tiền hàng không còn đạt mức tối thiểu)";
            } else {
                updatedDiscount = updatedRule.calculateDiscount(newSubtotal);
                orders.updateVoucherUsageDiscount(order.id(), updatedDiscount);
                if (order.discountAmount() != null && updatedDiscount.compareTo(order.discountAmount()) != 0) {
                    voucherAuditNote = " (Cập nhật giảm giá " + updatedVoucherCode + ": "
                            + order.discountAmount() + "đ -> " + updatedDiscount + "đ)";
                }
            }
        }

        PricingBreakdown updatedPricing = PricingBreakdown.of(
                newSubtotal, shippingFee, taxAmount, updatedVoucherCode, updatedDiscount);
        orders.updateItemQuantityAndPricing(
                order,
                product,
                request.quantity(),
                updatedPricing,
                updatedPricing.voucherCode() == null ? null : updatedRule);

        orders.history(id, order.status().name(), order.status().name(), actor,
                "COD " + product + ": " + existingLine.quantity() + " -> " + request.quantity()
                        + ". " + request.reason() + voucherAuditNote);
        log.info("Order quantity adjusted: orderId={}, productId={}, oldQty={}, newQty={}, newSubtotal={}, newDiscount={}, newTotal={}",
                id, product, existingLine.quantity(), request.quantity(),
                updatedPricing.subtotal(), updatedPricing.discountAmount(), updatedPricing.total());
        return orders.get(id, false);
    }

    public void expire(UUID id) {
        Order order = orders.get(id, true);
        if ("UNPAID".equals(order.paymentStatus())
                && (order.status() == OrderStatus.PENDING_PAYMENT || order.status() == OrderStatus.PENDING_CONFIRMATION)
                && !clock.instant().isBefore(order.expiresAt())) {
            release(order);
            orders.releaseVoucher(id);
            orders.state(order, OrderStatus.EXPIRED, "RELEASED");
            orders.paymentStatus(order, "EXPIRED");
            orders.history(id, order.status().name(), "EXPIRED", null, "Hết thời gian chờ");
            events.publishEvent(new OrderNotification(order.email(), order.code(), "EXPIRED"));
            log.info("Expired unpaid order and released reservation: orderId={}", id);
        }
    }

    private VoucherRuleSnapshot resolveOrderVoucherRule(Order order) {
        if (order.voucherRule() != null) {
            return order.voucherRule();
        }
        if (order.voucherCode() == null || order.voucherCode().isBlank()) {
            return null;
        }
        return orders.findVoucher(order.voucherCode()).map(OrderRepository.Voucher::toRuleSnapshot).orElse(null);
    }

    private void deductOrderedItemsFromCart(CartSession cart, List<Line> orderedLines) {
        for (Line line : orderedLines) {
            Integer current = cart.quantities().get(line.productId());
            if (current != null) {
                int remaining = current - line.quantity();
                if (remaining > 0) {
                    cart.quantities().put(line.productId(), remaining);
                } else {
                    cart.quantities().remove(line.productId());
                }
            }
        }
        if (cart.quantities().isEmpty()) {
            cart.clearBuildCheckout();
        }
        cart.incrementVersion();
    }

    private Map<UUID, Integer> resolveSelectedCartItems(CartSession cart, List<UUID> selectedProductIds) {
        if (cart.quantities().isEmpty()) {
            throw OrderException.conflict("EMPTY_CART", "Giỏ hàng của bạn đang trống, không thể đặt hàng.");
        }
        if (cart.quantities().size() > 100) {
            throw OrderException.conflict("INVALID_CART", "Giỏ hàng chỉ hỗ trợ tối đa 100 sản phẩm.");
        }
        if (selectedProductIds == null) {
            return new LinkedHashMap<>(cart.quantities());
        }
        if (selectedProductIds.isEmpty()) {
            throw OrderException.badRequest("NO_ITEMS_SELECTED", "Vui lòng chọn ít nhất một sản phẩm trong giỏ hàng để đặt mua.");
        }
        Map<UUID, Integer> selected = new LinkedHashMap<>();
        for (UUID productId : selectedProductIds) {
            if (productId == null) {
                throw OrderException.badRequest("INVALID_SELECTED_ITEM", "Mã sản phẩm chọn mua không hợp lệ.");
            }
            if (selected.containsKey(productId)) {
                throw OrderException.badRequest("DUPLICATE_SELECTED_ITEM", "Danh sách sản phẩm chọn mua bị trùng lặp.");
            }
            Integer quantity = cart.quantities().get(productId);
            if (quantity == null) {
                throw OrderException.conflict("CART_ITEM_NOT_IN_CART", "Sản phẩm được chọn không nằm trong giỏ hàng của bạn.");
            }
            selected.put(productId, quantity);
        }
        return selected;
    }

    private void validateVoucherStrict(OrderRepository.Voucher voucher, UUID actor, BigDecimal subtotal, Instant now) {
        Optional<String> invalidReason = checkVoucherInvalidReason(voucher, actor, subtotal, now);
        if (invalidReason.isPresent()) {
            throw OrderException.conflict("VOUCHER_INVALID", invalidReason.get());
        }
    }

    private Optional<String> checkVoucherInvalidReason(OrderRepository.Voucher voucher, UUID actor, BigDecimal subtotal, Instant now) {
        if (!voucher.active()) {
            return Optional.of("Mã giảm giá \"" + voucher.code() + "\" đã ngừng áp dụng.");
        }
        if (now.isBefore(voucher.startsAt())) {
            return Optional.of("Mã giảm giá \"" + voucher.code() + "\" chưa đến thời gian có hiệu lực.");
        }
        if (!now.isBefore(voucher.expiresAt())) {
            return Optional.of("Mã giảm giá \"" + voucher.code() + "\" đã hết hạn.");
        }
        if (subtotal.compareTo(voucher.minOrderSubtotal()) < 0) {
            return Optional.of("Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.minOrderSubtotal().toPlainString() + "đ) để áp dụng mã \"" + voucher.code() + "\".");
        }
        if (voucher.usageLimit() != null && voucher.usedCount() >= voucher.usageLimit()) {
            return Optional.of("Mã giảm giá \"" + voucher.code() + "\" đã hết lượt sử dụng.");
        }
        int customerUsed = orders.countCustomerVoucherUsage(voucher.code(), actor);
        if (customerUsed >= voucher.perUserLimit()) {
            return Optional.of("Bạn đã sử dụng tối đa số lượt cho phép của mã giảm giá \"" + voucher.code() + "\".");
        }
        return Optional.empty();
    }

    private BigDecimal sumSubtotal(List<Line> lines) {
        return lines.stream().map(Line::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("[a-zA-Z0-9_-]{8,100}")) {
            throw new OrderException(HttpStatus.BAD_REQUEST,
                    "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key phải dài 8–100 ký tự chữ, số, _ hoặc -.");
        }
    }

    private Order owned(UUID actor, UUID id, boolean admin, boolean lock) {
        Order order = orders.get(id, lock);
        if (!admin && !actor.equals(order.customerId())) throw OrderException.missing();
        return order;
    }

    private void release(Order order) {
        if (!"HELD".equals(order.reservationStatus())) {
            throw OrderException.conflict("RESERVATION_CONFLICT", "Đơn không còn giữ hàng.");
        }
        for (Line line : sorted(order.items())) {
            orders.lockProduct(line.productId(), line.quantity());
            orders.reserve(line.productId(), -line.quantity());
        }
    }

    private List<Line> sorted(List<Line> lines) {
        return lines.stream().sorted(Comparator.comparing(line -> line.productId().toString())).toList();
    }

    private void requireValidCartVersion(CartSession cart, long version) {
        if (cart.version() != version) {
            throw OrderException.conflict("CART_VERSION_CONFLICT", "Giỏ hàng đã thay đổi. Vui lòng tải lại giỏ hàng.");
        }
    }

    private Optional<Map<String, Object>> inspectProductIssue(OrderRepository.Stock stock, int quantity) {
        if (stock == null || stock.line() == null) {
            return Optional.of(Map.of("requestedQuantity", quantity, "availableQuantity", 0, "reason", "PRODUCT_DELETED"));
        }
        if (!"ACTIVE".equalsIgnoreCase(stock.status())) {
            return Optional.of(Map.of(
                    "productId", stock.line().productId(),
                    "name", stock.line().name(),
                    "requestedQuantity", quantity,
                    "availableQuantity", 0,
                    "reason", "PRODUCT_UNAVAILABLE"));
        }
        if (stock.line().unitPrice() == null
                || stock.line().unitPrice().signum() <= 0
                || stock.line().unitPrice().stripTrailingZeros().scale() > 2) {
            return Optional.of(Map.of(
                    "productId", stock.line().productId(),
                    "name", stock.line().name(),
                    "requestedQuantity", quantity,
                    "availableQuantity", Math.max(0, stock.available()),
                    "reason", "INVALID_PRODUCT_PRICE"));
        }
        int available = Math.max(0, stock.available());
        if (available < quantity) {
            return Optional.of(Map.of(
                    "productId", stock.line().productId(),
                    "name", stock.line().name(),
                    "requestedQuantity", quantity,
                    "availableQuantity", available,
                    "reason", "INSUFFICIENT_STOCK"));
        }
        return Optional.empty();
    }

    private void throwIfAnyProductIssues(List<Map<String, Object>> itemIssues) {
        if (itemIssues.isEmpty()) return;
        Map<String, Object> first = itemIssues.getFirst();
        String primaryCode = String.valueOf(first.get("reason"));
        String name = first.get("name") == null ? "" : "\"" + first.get("name") + "\" ";
        int available = first.get("availableQuantity") instanceof Number n ? n.intValue() : 0;
        int requested = first.get("requestedQuantity") instanceof Number n ? n.intValue() : 0;

        String message = switch (primaryCode) {
            case "PRODUCT_DELETED" -> "Sản phẩm " + name + "không còn tồn tại trên hệ thống. Vui lòng bỏ chọn sản phẩm này.";
            case "PRODUCT_UNAVAILABLE" -> "Sản phẩm " + name + "hiện đã ngừng bán.";
            case "INVALID_PRODUCT_PRICE" -> "Giá sản phẩm " + name + "không hợp lệ.";
            default -> available == 0
                    ? "Sản phẩm " + name + "hiện đã hết hàng (khả dụng: 0)."
                    : "Sản phẩm " + name + "chỉ còn " + available + " sản phẩm khả dụng (bạn đang chọn mua " + requested + ").";
        };

        Map<String, Object> details = new LinkedHashMap<>();
        if (first.get("productId") != null) details.put("productId", first.get("productId"));
        details.put("availableQuantity", available);
        details.put("requestedQuantity", requested);
        details.put("issues", List.copyOf(itemIssues));
        throw OrderException.conflict(primaryCode, message, details);
    }

    private void requireVersion(Order order, long version) {
        if (order.version() != version) {
            throw OrderException.conflict("ORDER_VERSION_CONFLICT", "Đơn đã được cập nhật. Hãy tải lại.");
        }
    }

    private void requireNotExpired(Order order) {
        if ("UNPAID".equals(order.paymentStatus())
                && (order.status() == OrderStatus.PENDING_PAYMENT || order.status() == OrderStatus.PENDING_CONFIRMATION)
                && !clock.instant().isBefore(order.expiresAt())) {
            throw OrderException.conflict("ORDER_EXPIRED", "Đơn đã hết hạn chờ.");
        }
    }
}
