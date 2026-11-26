package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.util.*;
import static iuh.fit.nhom16.pcstore.pcbuild.PcBuildDtos.*;

@Component
public class PcBuildInventory {
    private final JdbcTemplate jdbc;
    public PcBuildInventory(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void validateLocked(List<Line> lines) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Cart confirmation requires a transaction");
        List<UUID> ids = lines.stream().map(Line::productId).sorted().toList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        // Stable lock order avoids two confirmations locking product rows in opposite order.
        var rows = jdbc.query("SELECT id, price, status, stock_on_hand, reserved_quantity, version "
                        + "FROM products WHERE id IN (" + placeholders + ") ORDER BY id FOR UPDATE",
                (rs, row) -> new Stock(rs.getObject("id", UUID.class), rs.getBigDecimal("price"),
                        rs.getString("status"), rs.getInt("stock_on_hand") - rs.getInt("reserved_quantity"),
                        rs.getLong("version")), ids.toArray());
        var byId = new HashMap<UUID,Stock>();
        rows.forEach(row -> byId.put(row.id(), row));
        validate(lines, byId);
    }
    static void validate(List<Line> lines, Map<UUID,Stock> products) {
        for (Line line : lines) {
            Stock product = products.get(line.productId());
            if (product == null || !"ACTIVE".equals(product.status()) || product.available() < line.quantity())
                throw conflict("PC_BUILD_STOCK_CHANGED", "Sản phẩm không còn đủ hàng. Hãy lấy đề xuất mới.");
            if (product.price().compareTo(line.unitPrice()) != 0 || product.version() != line.productVersion())
                throw conflict("PC_BUILD_PRICE_CHANGED", "Giá hoặc sản phẩm đã thay đổi. Hãy xem và xác nhận đề xuất mới.");
        }
    }
    static CartException conflict(String code, String message) {
        return new CartException(HttpStatus.CONFLICT, code, message);
    }
    record Stock(UUID id, BigDecimal price, String status, int available, long version) {}
}
