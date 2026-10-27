package iuh.fit.nhom16.pcstore.sales.repository;

import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.*;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import iuh.fit.nhom16.pcstore.sales.model.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@Repository
public class OrderRepository {
    private final JdbcTemplate jdbc;
    public OrderRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Stock(Line line, int available, String status) { }
    public Stock lockProduct(UUID id, int quantity) {
        var rows = jdbc.query("SELECT * FROM products WHERE id = ? FOR UPDATE", (rs, n) ->
                new Stock(new Line(id, rs.getString("name"), rs.getString("sku"), rs.getBigDecimal("price"),
                        quantity, rs.getInt("warranty_months")), rs.getInt("stock_on_hand") - rs.getInt("reserved_quantity"),
                        rs.getString("status")), id);
        if (rows.isEmpty()) throw OrderException.conflict("PRODUCT_UNAVAILABLE", "Sản phẩm không còn khả dụng.");
        return rows.getFirst();
    }

    public void saveQuote(Quote quote) {
        jdbc.update("""
                INSERT INTO sales_quotes(id,customer_id,cart_version,recipient,phone,address,shipping_fee,expires_at)
                VALUES (?,?,?,?,?,?,?,?)
                """, quote.id(), quote.customerId(), quote.cartVersion(), quote.recipient(), quote.phone(),
                quote.address(), quote.shippingFee(), Timestamp.from(quote.expiresAt()));
        for (Line line : quote.items()) jdbc.update("INSERT INTO sales_quote_items VALUES (?,?,?,?,?,?,?)",
                quote.id(), line.productId(), line.name(), line.sku(), line.unitPrice(), line.quantity(), line.warrantyMonths());
    }
    public Quote lockQuote(UUID id, UUID customer) {
        var rows = jdbc.query("SELECT * FROM sales_quotes WHERE id = ? AND customer_id = ? FOR UPDATE", (rs, n) -> {
            if (rs.getBoolean("consumed")) throw OrderException.conflict("QUOTE_USED", "Bản xem trước đã được sử dụng.");
            return new Quote(id, customer, rs.getLong("cart_version"), rs.getString("recipient"), rs.getString("phone"),
                    rs.getString("address"), rs.getTimestamp("expires_at").toInstant(), quoteLines(id), rs.getBigDecimal("shipping_fee"));
        }, id, customer);
        if (rows.isEmpty()) throw OrderException.missing();
        return rows.getFirst();
    }
    private List<Line> quoteLines(UUID id) { return lines("sales_quote_items", "quote_id", id); }
    public List<Line> orderLines(UUID id) { return lines("sales_order_items", "order_id", id); }
    private List<Line> lines(String table, String column, UUID id) {
        // Table/column are internal constants, never supplied by requests.
        return jdbc.query("SELECT * FROM " + table + " WHERE " + column + " = ? ORDER BY product_id", (rs, n) ->
                new Line(rs.getObject("product_id", UUID.class), rs.getString("name"), rs.getString("sku"),
                        rs.getBigDecimal("unit_price"), rs.getInt("quantity"), rs.getInt("warranty_months")), id);
    }
    public Optional<UUID> replay(UUID customer, String key, PlaceRequest request) {
        return jdbc.query("SELECT * FROM sales_idempotency WHERE customer_id = ? AND key = ?", (rs, n) -> {
            if (!request.quoteId().equals(rs.getObject("quote_id", UUID.class)) ||
                    !request.paymentMethod().name().equals(rs.getString("payment_method")))
                throw OrderException.conflict("IDEMPOTENCY_CONFLICT", "Khóa đã được dùng cho yêu cầu khác.");
            return rs.getObject("order_id", UUID.class);
        }, customer, key).stream().findFirst();
    }
    public void create(UUID id, Quote quote, String email, PaymentMethod method, Instant expiry, String url) {
        String reference = id.toString().replace("-", "");
        jdbc.update("""
                INSERT INTO sales_orders(id,code,customer_id,quote_id,email,recipient,phone,address,shipping_fee,total,
                  payment_method,status,expires_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, "PC-" + reference, quote.customerId(), quote.id(), email, quote.recipient(), quote.phone(),
                quote.address(), quote.shippingFee(), quote.total(), method.name(),
                method == PaymentMethod.COD ? "PENDING_CONFIRMATION" : "PENDING_PAYMENT", Timestamp.from(expiry));
        for (Line line : quote.items()) jdbc.update("INSERT INTO sales_order_items VALUES (?,?,?,?,?,?,?)",
                id, line.productId(), line.name(), line.sku(), line.unitPrice(), line.quantity(), line.warrantyMonths());
        jdbc.update("INSERT INTO sales_payment_attempts(order_id,reference,amount,payment_url) VALUES (?,?,?,?)",
                id, reference, quote.total(), url);
        jdbc.update("UPDATE sales_quotes SET consumed = TRUE WHERE id = ?", quote.id());
    }
    public void remember(UUID customer, String key, PlaceRequest request, UUID order) {
        jdbc.update("INSERT INTO sales_idempotency VALUES (?,?,?,?,?)", customer, key, request.quoteId(), request.paymentMethod().name(), order);
    }
    public Order get(UUID id, boolean lock) {
        var rows = jdbc.query("SELECT * FROM sales_orders WHERE id = ?" + (lock ? " FOR UPDATE" : ""), (rs, n) -> {
            var payment = jdbc.queryForMap("SELECT * FROM sales_payment_attempts WHERE order_id = ?", id);
            return new Order(id, rs.getString("code"), rs.getObject("customer_id", UUID.class), rs.getString("email"),
                    rs.getString("recipient"), rs.getString("phone"), rs.getString("address"),
                    PaymentMethod.valueOf(rs.getString("payment_method")), OrderStatus.valueOf(rs.getString("status")),
                    rs.getString("payment_status"), rs.getString("reservation_status"), rs.getLong("version"),
                    rs.getTimestamp("expires_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                    rs.getBigDecimal("shipping_fee"), rs.getBigDecimal("total"), (String) payment.get("reference"),
                    (String) payment.get("payment_url"), orderLines(id), history(id));
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
        int changed = jdbc.update("""
                UPDATE products SET reserved_quantity = reserved_quantity + ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND reserved_quantity + ? BETWEEN 0 AND stock_on_hand
                """, delta, product, delta);
        if (changed != 1) throw OrderException.conflict("INSUFFICIENT_STOCK", "Không đủ tồn kho khả dụng.");
    }
    public void consume(UUID product, int quantity) {
        int changed = jdbc.update("""
                UPDATE products SET stock_on_hand = stock_on_hand - ?, reserved_quantity = reserved_quantity - ?,
                version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND reserved_quantity >= ? AND stock_on_hand >= ?
                """, quantity, quantity, product, quantity, quantity);
        if (changed != 1) throw OrderException.conflict("INVENTORY_CONFLICT", "Tồn kho cần được kiểm tra lại.");
    }
    public void state(Order order, OrderStatus status, String reservation) {
        jdbc.update("UPDATE sales_orders SET status=?, reservation_status=?, version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=?",
                status.name(), reservation, order.id());
    }
    public void history(UUID id, String from, String to, UUID actor, String reason) {
        jdbc.update("INSERT INTO sales_history(order_id,from_status,to_status,actor_id,reason) VALUES (?,?,?,?,?)", id, from, to, actor, reason);
    }
    private List<History> history(UUID id) {
        return jdbc.query("SELECT * FROM sales_history WHERE order_id = ? ORDER BY id", (rs, n) ->
                new History(rs.getString("from_status"), rs.getString("to_status"), rs.getObject("actor_id", UUID.class),
                        rs.getString("reason"), rs.getTimestamp("created_at").toInstant()), id);
    }
    public void quantity(Order order, UUID product, int quantity, BigDecimal total) {
        jdbc.update("UPDATE sales_order_items SET quantity=? WHERE order_id=? AND product_id=?", quantity, order.id(), product);
        jdbc.update("UPDATE sales_orders SET total=?, version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=?", total, order.id());
        jdbc.update("UPDATE sales_payment_attempts SET amount=? WHERE order_id=?", total, order.id());
    }
    public Optional<UUID> findPayment(String reference) {
        return jdbc.queryForList("SELECT order_id FROM sales_payment_attempts WHERE reference=?", UUID.class, reference).stream().findFirst();
    }
    public void payment(Order order, String status, String transaction) {
        jdbc.update("UPDATE sales_orders SET payment_status=?, version=version+1, updated_at=CURRENT_TIMESTAMP WHERE id=?", status, order.id());
        jdbc.update("UPDATE sales_payment_attempts SET status=?, transaction_code=? WHERE order_id=?", status, transaction, order.id());
    }
    public void paymentEvent(UUID id, String transaction, String response, String status) {
        jdbc.update("INSERT INTO sales_payment_events(order_id,transaction_code,response_code,transaction_status) VALUES (?,?,?,?)", id, transaction, response, status);
    }
}
