package iuh.fit.nhom16.pcstore.cart.repository;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

@Repository
public class CartRepository {
    private final JdbcTemplate jdbc;
    public CartRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }


    public CartSession lock(UUID userId) {
        jdbc.update("INSERT INTO carts(id, user_id) VALUES (?, ?) ON CONFLICT (user_id) DO NOTHING",
                UUID.randomUUID(), userId);
        return jdbc.queryForObject("SELECT id, version FROM carts WHERE user_id = ? FOR UPDATE",
                (rs, row) -> {
                    UUID id = rs.getObject("id", UUID.class);
                    var quantities = new LinkedHashMap<UUID, Integer>();
                    jdbc.query("SELECT product_id, quantity FROM cart_items WHERE cart_id = ? ORDER BY product_id",
                            (org.springframework.jdbc.core.RowCallbackHandler) item ->
                                    quantities.put(item.getObject("product_id", UUID.class), item.getInt("quantity")), id);
                    return new CartSession(id, rs.getLong("version"), quantities);
                }, userId);
    }

    public void save(CartSession cart) {
        if (cart.quantities().isEmpty()) {
            jdbc.update("DELETE FROM cart_items WHERE cart_id = ?", cart.mergeId());
        } else {
            var parameters = new ArrayList<Object>();
            parameters.add(cart.mergeId());
            parameters.addAll(cart.quantities().keySet());
            String placeholders = String.join(",", Collections.nCopies(cart.quantities().size(), "?"));
            jdbc.update("DELETE FROM cart_items WHERE cart_id = ? AND product_id NOT IN (" + placeholders + ")",
                    parameters.toArray());
            jdbc.batchUpdate("""
                    INSERT INTO cart_items(id, cart_id, product_id, quantity) VALUES (?, ?, ?, ?)
                    ON CONFLICT (cart_id, product_id) DO UPDATE SET quantity = EXCLUDED.quantity
                    WHERE cart_items.quantity IS DISTINCT FROM EXCLUDED.quantity
                    """, cart.quantities().entrySet(), 20, (statement, entry) -> {
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, cart.mergeId());
                statement.setObject(3, entry.getKey());
                statement.setInt(4, entry.getValue());
            });
        }
        jdbc.update("UPDATE carts SET version = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                cart.version(), cart.mergeId());
    }

    public boolean claimMerge(UUID guestId, UUID cartId) {
        return jdbc.update("INSERT INTO cart_merge_receipts(guest_id, cart_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                guestId, cartId) == 1;
    }

    public boolean activeUser(UUID userId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM users WHERE id = ? AND status = 'ACTIVE')", Boolean.class, userId));
    }
}
