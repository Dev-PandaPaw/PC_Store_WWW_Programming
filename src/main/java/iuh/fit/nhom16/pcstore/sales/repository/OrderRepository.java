package iuh.fit.nhom16.pcstore.sales.repository;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository("salesJdbcOrderRepository")
public class OrderRepository {
    private final JdbcTemplate jdbc;

    public OrderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Stock(Line line, int available, String status, long version) {
        public Stock(Line line, int available, String status) {
            this(line, available, status, 0L);
        }
    }

    public record Voucher(
            String code,
            String description,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minOrderSubtotal,
            BigDecimal maxDiscountAmount,
            Integer usageLimit,
            int usedCount,
            int perUserLimit,
            boolean active,
            Instant startsAt,
            Instant expiresAt,
            long version) {

        public VoucherRuleSnapshot toRuleSnapshot() {
            return new VoucherRuleSnapshot(code, discountType, discountValue, minOrderSubtotal, maxDiscountAmount);
        }
    }

    public Stock lockProduct(UUID id, int quantity) {
        var rows = jdbc.query("SELECT * FROM products WHERE id = ? FOR UPDATE", (rs, n) ->
                new Stock(
                        new Line(
                                id,
                                rs.getString("name"),
                                rs.getString("sku"),
                                rs.getBigDecimal("price"),
                                quantity,
                                rs.getObject("warranty_months") == null ? 12 : rs.getInt("warranty_months")),
                        rs.getInt("stock_on_hand") - rs.getInt("reserved_quantity"),
                        rs.getString("status"),
                        rs.getObject("version") == null ? 0L : rs.getLong("version")),
                id);
        if (rows.isEmpty()) {
            throw OrderException.conflict(
                    "PRODUCT_DELETED",
                    "Sản phẩm không còn tồn tại hoặc đã bị xóa khỏi hệ thống.",
                    Map.of("productId", id));
        }
        return rows.getFirst();
    }

    public Optional<Voucher> findVoucher(String code) {
        return queryVoucher("SELECT * FROM sales_vouchers WHERE code = ?", code);
    }

    public Optional<Voucher> lockVoucher(String code) {
        return queryVoucher("SELECT * FROM sales_vouchers WHERE code = ? FOR UPDATE", code);
    }

    private Optional<Voucher> queryVoucher(String sql, String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return jdbc.query(sql, (rs, n) -> new Voucher(
                rs.getString("code"),
                rs.getString("description"),
                rs.getString("discount_type"),
                rs.getBigDecimal("discount_value"),
                rs.getBigDecimal("min_order_subtotal"),
                rs.getBigDecimal("max_discount_amount"),
                rs.getObject("usage_limit", Integer.class),
                rs.getInt("used_count"),
                rs.getInt("per_user_limit"),
                rs.getBoolean("active"),
                rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getLong("version")), code.trim().toUpperCase()).stream().findFirst();
    }

    public int countCustomerVoucherUsage(String code, UUID customerId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM sales_voucher_usages WHERE voucher_code = ? AND customer_id = ?",
                Integer.class, code, customerId);
        return count == null ? 0 : count;
    }

    public List<VoucherView> activeVouchers(Instant now) {
        return jdbc.query("""
                SELECT code, description, discount_type, discount_value, min_order_subtotal, max_discount_amount, expires_at
                FROM sales_vouchers
                WHERE active = TRUE
                  AND starts_at <= ?
                  AND expires_at > ?
                  AND (usage_limit IS NULL OR used_count < usage_limit)
                ORDER BY min_order_subtotal ASC, code ASC
                """, (rs, n) -> new VoucherView(
                rs.getString("code"),
                rs.getString("description"),
                rs.getString("discount_type"),
                rs.getBigDecimal("discount_value"),
                rs.getBigDecimal("min_order_subtotal"),
                rs.getBigDecimal("max_discount_amount"),
                rs.getTimestamp("expires_at").toInstant()),
                Timestamp.from(now), Timestamp.from(now));
    }

    public void consumeVoucher(String code, long expectedVersion, UUID orderId, UUID customerId, BigDecimal discountAmount) {
        int changed = jdbc.update("""
                UPDATE sales_vouchers
                SET used_count = used_count + 1, version = version + 1
                WHERE code = ?
                  AND active = TRUE
                  AND version = ?
                  AND (usage_limit IS NULL OR used_count < usage_limit)
                """, code, expectedVersion);
        if (changed != 1) {
            throw OrderException.conflict("VOUCHER_EXHAUSTED", "Mã giảm giá vừa hết lượt sử dụng hoặc đã thay đổi.");
        }
        jdbc.update("""
                INSERT INTO sales_voucher_usages(order_id, voucher_code, customer_id, discount_amount)
                VALUES (?, ?, ?, ?)
                """, orderId, code, customerId, discountAmount);
    }

    public void updateVoucherUsageDiscount(UUID orderId, BigDecimal discountAmount) {
        jdbc.update("UPDATE sales_voucher_usages SET discount_amount = ? WHERE order_id = ?", discountAmount, orderId);
    }

    public void releaseVoucher(UUID orderId) {
        var codes = jdbc.queryForList(
                "SELECT voucher_code FROM sales_voucher_usages WHERE order_id = ? FOR UPDATE",
                String.class, orderId);
        if (!codes.isEmpty()) {
            String code = codes.getFirst();
            jdbc.update("DELETE FROM sales_voucher_usages WHERE order_id = ?", orderId);
            jdbc.update("UPDATE sales_vouchers SET used_count = GREATEST(0, used_count - 1), version = version + 1 WHERE code = ?", code);
        }
    }

    public void saveQuote(Quote quote) {
        VoucherRuleSnapshot rule = quote.voucherRule();
        jdbc.update("""
                INSERT INTO sales_quotes(
                    id, customer_id, cart_version, recipient, phone, address,
                    province_code, district_code, ward_code,
                    shipping_method, payment_method, subtotal, shipping_fee, tax_amount,
                    voucher_code, voucher_discount_type, voucher_discount_value, voucher_min_subtotal, voucher_max_discount,
                    discount_amount, total, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                quote.id(),
                quote.customerId(),
                quote.cartVersion(),
                quote.recipient(),
                quote.phone(),
                quote.address(),
                quote.provinceCode(),
                quote.districtCode(),
                quote.wardCode(),
                (quote.shippingMethod() == null ? ShippingMethod.EXPRESS : quote.shippingMethod()).name(),
                (quote.paymentMethod() == null ? PaymentMethod.COD : quote.paymentMethod()).name(),
                quote.subtotal(),
                quote.shippingFee(),
                quote.taxAmount() == null ? BigDecimal.ZERO : quote.taxAmount(),
                quote.voucherCode(),
                rule == null ? null : rule.discountType(),
                rule == null ? null : rule.discountValue(),
                rule == null ? null : rule.minOrderSubtotal(),
                rule == null ? null : rule.maxDiscountAmount(),
                quote.discountAmount() == null ? BigDecimal.ZERO : quote.discountAmount(),
                quote.total(),
                Timestamp.from(quote.expiresAt()));

        for (Line line : quote.items()) {
            jdbc.update("""
                    INSERT INTO sales_quote_items(quote_id, product_id, name, sku, unit_price, quantity, warranty_months, line_total)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    quote.id(), line.productId(), line.name(), line.sku(),
                    line.unitPrice(), line.quantity(), line.warrantyMonths(), line.subtotal());
        }
    }

    public Quote lockQuote(UUID id, UUID customer) {
        var rows = jdbc.query("SELECT * FROM sales_quotes WHERE id = ? AND customer_id = ? FOR UPDATE", (rs, n) -> {
            if (rs.getBoolean("consumed")) {
                throw OrderException.conflict("QUOTE_USED", "Bản xem trước đơn hàng đã được sử dụng.");
            }
            List<Line> items = quoteLines(id);
            BigDecimal shippingFee = rs.getBigDecimal("shipping_fee");
            BigDecimal subtotal = rs.getBigDecimal("subtotal");
            BigDecimal taxAmount = rs.getBigDecimal("tax_amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("tax_amount");
            BigDecimal discountAmount = rs.getBigDecimal("discount_amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("discount_amount");
            BigDecimal total = rs.getBigDecimal("total");
            String voucherCode = rs.getString("voucher_code");
            String ruleType = rs.getString("voucher_discount_type");
            VoucherRuleSnapshot rule = (voucherCode != null && ruleType != null)
                    ? new VoucherRuleSnapshot(
                            voucherCode,
                            ruleType,
                            rs.getBigDecimal("voucher_discount_value"),
                            rs.getBigDecimal("voucher_min_subtotal"),
                            rs.getBigDecimal("voucher_max_discount"))
                    : null;
            String shipMethodRaw = rs.getString("shipping_method");
            ShippingMethod shipMethod = shipMethodRaw == null ? ShippingMethod.EXPRESS : ShippingMethod.valueOf(shipMethodRaw);
            String payMethodRaw = rs.getString("payment_method");
            PaymentMethod payMethod = payMethodRaw == null ? PaymentMethod.COD : PaymentMethod.valueOf(payMethodRaw);
            return new Quote(
                    id,
                    customer,
                    rs.getLong("cart_version"),
                    rs.getString("recipient"),
                    rs.getString("phone"),
                    rs.getString("address"),
                    rs.getObject("province_code", Integer.class),
                    rs.getObject("district_code", Integer.class),
                    rs.getObject("ward_code", Integer.class),
                    shipMethod,
                    payMethod,
                    rs.getTimestamp("expires_at").toInstant(),
                    items,
                    subtotal,
                    shippingFee,
                    taxAmount,
                    voucherCode,
                    rule,
                    discountAmount,
                    total);
        }, id, customer);
        if (rows.isEmpty()) throw OrderException.missing();
        return rows.getFirst();
    }

    private List<Line> quoteLines(UUID id) {
        return lines("sales_quote_items", "quote_id", id);
    }

    public List<Line> orderLines(UUID id) {
        return lines("sales_order_items", "order_id", id);
    }

    private List<Line> lines(String table, String column, UUID id) {
        // Table/column are internal constants, never supplied by requests.
        return jdbc.query("SELECT * FROM " + table + " WHERE " + column + " = ? ORDER BY product_id", (rs, n) ->
                new Line(
                        rs.getObject("product_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("sku"),
                        rs.getBigDecimal("unit_price"),
                        rs.getInt("quantity"),
                        rs.getInt("warranty_months")), id);
    }

    public Optional<UUID> replay(UUID customer, String key, PlaceRequest request) {
        return jdbc.query("SELECT * FROM sales_idempotency WHERE customer_id = ? AND key = ?", (rs, n) -> {
            if (!request.quoteId().equals(rs.getObject("quote_id", UUID.class))
                    || !request.paymentMethod().name().equals(rs.getString("payment_method"))) {
                throw OrderException.conflict("IDEMPOTENCY_CONFLICT", "Khóa định danh thao tác đã được dùng cho yêu cầu đặt hàng khác.");
            }
            return rs.getObject("order_id", UUID.class);
        }, customer, key).stream().findFirst();
    }

    public Optional<UUID> findOrderByIdempotencyKey(UUID customer, String key) {
        return jdbc.query(
                "SELECT order_id FROM sales_idempotency WHERE customer_id = ? AND key = ?",
                (rs, n) -> rs.getObject("order_id", UUID.class),
                customer, key).stream().findFirst();
    }

    public void create(UUID id, Quote quote, PaymentMethod paymentMethod, OrderStatus initialStatus, String email, Instant expiry) {
        if (quote.items() == null || quote.items().isEmpty()) {
            throw OrderException.conflict("EMPTY_ORDER_ITEMS", "Đơn hàng phải có ít nhất một sản phẩm chi tiết.");
        }
        VoucherRuleSnapshot rule = quote.voucherRule();
        String reference = id.toString().replace("-", "");
        jdbc.update("""
                INSERT INTO sales_orders(
                    id, code, customer_id, quote_id, email, recipient, phone, address,
                    province_code, district_code, ward_code,
                    shipping_method, subtotal, shipping_fee, tax_amount,
                    voucher_code, voucher_discount_type, voucher_discount_value, voucher_min_subtotal, voucher_max_discount,
                    discount_amount, total, payment_method, status, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "PC-" + reference,
                quote.customerId(),
                quote.id(),
                email,
                quote.recipient(),
                quote.phone(),
                quote.address(),
                quote.provinceCode(),
                quote.districtCode(),
                quote.wardCode(),
                (quote.shippingMethod() == null ? ShippingMethod.EXPRESS : quote.shippingMethod()).name(),
                quote.subtotal(),
                quote.shippingFee(),
                quote.taxAmount() == null ? BigDecimal.ZERO : quote.taxAmount(),
                quote.voucherCode(),
                rule == null ? null : rule.discountType(),
                rule == null ? null : rule.discountValue(),
                rule == null ? null : rule.minOrderSubtotal(),
                rule == null ? null : rule.maxDiscountAmount(),
                quote.discountAmount() == null ? BigDecimal.ZERO : quote.discountAmount(),
                quote.total(),
                paymentMethod.name(),
                initialStatus.name(),
                Timestamp.from(expiry));

        int insertedItems = 0;
        for (Line line : quote.items()) {
            insertedItems += jdbc.update("""
                    INSERT INTO sales_order_items(order_id, product_id, name, sku, unit_price, quantity, warranty_months, line_total)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    id, line.productId(), line.name(), line.sku(),
                    line.unitPrice(), line.quantity(), line.warrantyMonths(), line.subtotal());
        }
        if (insertedItems != quote.items().size()) {
            throw new IllegalStateException("Failed to persist all order items for order " + id);
        }

        int consumed = jdbc.update("UPDATE sales_quotes SET consumed = TRUE WHERE id = ? AND consumed = FALSE", quote.id());
        if (consumed != 1) {
            throw OrderException.conflict("QUOTE_USED", "Bản xem trước đơn hàng đã được sử dụng.");
        }
    }

    public void remember(UUID customer, String key, PlaceRequest request, UUID order) {
        jdbc.update("INSERT INTO sales_idempotency VALUES (?,?,?,?,?)",
                customer, key, request.quoteId(), request.paymentMethod().name(), order);
    }

    public Order get(UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM sales_orders WHERE id = ?" + (lock ? " FOR UPDATE" : ""), (rs, n) -> {
            List<Line> items = orderLines(id);
            BigDecimal subtotal = rs.getBigDecimal("subtotal");
            String shipMethodRaw = rs.getString("shipping_method");
            ShippingMethod shipMethod = shipMethodRaw == null ? ShippingMethod.EXPRESS : ShippingMethod.valueOf(shipMethodRaw);
            BigDecimal taxAmount = rs.getBigDecimal("tax_amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("tax_amount");
            BigDecimal discountAmount = rs.getBigDecimal("discount_amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("discount_amount");
            String voucherCode = rs.getString("voucher_code");
            String ruleType = rs.getString("voucher_discount_type");
            VoucherRuleSnapshot rule = (voucherCode != null && ruleType != null)
                    ? new VoucherRuleSnapshot(
                            voucherCode,
                            ruleType,
                            rs.getBigDecimal("voucher_discount_value"),
                            rs.getBigDecimal("voucher_min_subtotal"),
                            rs.getBigDecimal("voucher_max_discount"))
                    : null;
            return new Order(
                    id,
                    rs.getString("code"),
                    rs.getObject("customer_id", UUID.class),
                    rs.getString("email"),
                    rs.getString("recipient"),
                    rs.getString("phone"),
                    rs.getString("address"),
                    rs.getObject("province_code", Integer.class),
                    rs.getObject("district_code", Integer.class),
                    rs.getObject("ward_code", Integer.class),
                    shipMethod,
                    PaymentMethod.valueOf(rs.getString("payment_method")),
                    OrderStatus.valueOf(rs.getString("status")),
                    rs.getString("payment_status"),
                    rs.getString("reservation_status"),
                    rs.getLong("version"),
                    rs.getTimestamp("expires_at").toInstant(),
                    rs.getTimestamp("created_at").toInstant(),
                    subtotal,
                    rs.getBigDecimal("shipping_fee"),
                    taxAmount,
                    voucherCode,
                    rule,
                    discountAmount,
                    rs.getBigDecimal("total"),
                    items,
                    history(id));
        }, id);
        if (rows.isEmpty()) throw OrderException.missing();
        return rows.getFirst();
    }

    public List<UUID> list(UUID customer, int page) {
        return customer == null
                ? jdbc.queryForList("SELECT id FROM sales_orders ORDER BY created_at DESC, id LIMIT 20 OFFSET ?", UUID.class, page * 20L)
                : jdbc.queryForList("SELECT id FROM sales_orders WHERE customer_id = ? ORDER BY created_at DESC, id LIMIT 20 OFFSET ?", UUID.class, customer, page * 20L);
    }

    public List<UUID> expired(Instant now) {
        return jdbc.queryForList("""
                SELECT id FROM sales_orders WHERE status IN ('PENDING_PAYMENT','PENDING_CONFIRMATION')
                AND payment_status = 'UNPAID' AND expires_at <= ? ORDER BY expires_at LIMIT 100
                """, UUID.class, Timestamp.from(now));
    }

    public void reserve(UUID product, int delta) {
        int changed;
        if (delta > 0) {
            changed = jdbc.update("""
                    UPDATE products
                    SET reserved_quantity = reserved_quantity + ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND status = 'ACTIVE'
                      AND stock_on_hand > 0
                      AND reserved_quantity + ? BETWEEN 0 AND stock_on_hand
                    """, delta, product, delta);
        } else {
            changed = jdbc.update("""
                    UPDATE products
                    SET reserved_quantity = reserved_quantity + ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                      AND reserved_quantity + ? BETWEEN 0 AND stock_on_hand
                    """, delta, product, delta);
        }
        if (changed != 1) {
            throw OrderException.conflict("INSUFFICIENT_STOCK", "Không đủ tồn kho khả dụng hoặc sản phẩm không còn tồn tại.");
        }
    }

    public void consume(UUID product, int quantity) {
        int changed = jdbc.update("""
                UPDATE products
                SET stock_on_hand = stock_on_hand - ?, reserved_quantity = reserved_quantity - ?,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND reserved_quantity >= ? AND stock_on_hand >= ?
                """, quantity, quantity, product, quantity, quantity);
        if (changed != 1) {
            throw OrderException.conflict("INVENTORY_CONFLICT", "Tồn kho cần được kiểm tra lại.");
        }
    }

    public void state(Order order, OrderStatus status, String reservation) {
        jdbc.update("""
                UPDATE sales_orders
                SET status = ?, reservation_status = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, status.name(), reservation, order.id());
    }

    public void history(UUID id, String from, String to, UUID actor, String reason) {
        jdbc.update("INSERT INTO sales_history(order_id,from_status,to_status,actor_id,reason) VALUES (?,?,?,?,?)",
                id, from, to, actor, reason);
    }

    private List<History> history(UUID id) {
        return jdbc.query("SELECT * FROM sales_history WHERE order_id = ? ORDER BY id", (rs, n) ->
                new History(
                        rs.getString("from_status"),
                        rs.getString("to_status"),
                        rs.getObject("actor_id", UUID.class),
                        rs.getString("reason"),
                        rs.getTimestamp("created_at").toInstant()), id);
    }

    public void updateItemQuantityAndPricing(Order order, UUID product, int quantity,
                                             PricingBreakdown pricing, VoucherRuleSnapshot rule) {
        BigDecimal unitPrice = order.items().stream()
                .filter(i -> i.productId().equals(product))
                .map(Line::unitPrice)
                .findFirst()
                .orElseThrow(OrderException::missing);
        BigDecimal newLineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        jdbc.update("""
                UPDATE sales_order_items
                SET quantity = ?, line_total = ?
                WHERE order_id = ? AND product_id = ?
                """, quantity, newLineTotal, order.id(), product);

        jdbc.update("""
                UPDATE sales_orders
                SET subtotal = ?,
                    voucher_code = ?,
                    voucher_discount_type = ?,
                    voucher_discount_value = ?,
                    voucher_min_subtotal = ?,
                    voucher_max_discount = ?,
                    discount_amount = ?,
                    total = ?,
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                pricing.subtotal(),
                pricing.voucherCode(),
                rule == null ? null : rule.discountType(),
                rule == null ? null : rule.discountValue(),
                rule == null ? null : rule.minOrderSubtotal(),
                rule == null ? null : rule.maxDiscountAmount(),
                pricing.discountAmount(),
                pricing.total(),
                order.id());
    }

    public void paymentStatus(Order order, String status) {
        jdbc.update("UPDATE sales_orders SET payment_status = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status, order.id());
    }
}
