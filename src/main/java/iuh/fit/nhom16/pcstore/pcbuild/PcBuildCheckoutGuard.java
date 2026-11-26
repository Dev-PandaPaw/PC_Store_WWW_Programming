package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Line;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Server-only Stage B. Called within the order transaction before any reserve/create. */
@Component
public class PcBuildCheckoutGuard {
    private final JdbcTemplate jdbc;
    private final PcBuildClient python;
    public PcBuildCheckoutGuard(JdbcTemplate jdbc, PcBuildClient python) { this.jdbc = jdbc; this.python = python; }
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireReady(CartSession cart, List<Line> lines) {
        if (!cart.buildCheckoutRequired()) return;
        if (lines.isEmpty()) throw blocked();
        // Product rows are locked by OrderService in stable ID order. These
        // shared metadata locks also prevent spec/rule edits between check and commit.
        jdbc.execute("LOCK TABLE product_component_specs, pc_build_rules IN SHARE MODE");
        Map<UUID,Long> versions = new HashMap<>();
        List<Map<String,Object>> expected = new ArrayList<>();
        for (Line line : lines.stream().sorted(Comparator.comparing(l -> l.productId().toString())).toList()) {
            Map<String,Object> current = jdbc.queryForMap("SELECT price, version FROM products WHERE id = ? FOR UPDATE", line.productId());
            if (decimal(current.get("price")).compareTo(line.unitPrice()) != 0)
                throw OrderException.conflict("PC_BUILD_PRICE_CHANGED", "Giá đã thay đổi. Hãy tạo lại bản xem trước đặt hàng.");
            long version = decimal(current.get("version")).longValueExact();
            versions.put(line.productId(), version);
            expected.add(Map.of("product_id", line.productId().toString(), "unit_price_vnd", line.unitPrice().toPlainString(), "product_version", version));
        }
        Map<String,Object> response;
        try {
            response = python.checkout(Map.of("cart_id", cart.mergeId().toString(), "cart_snapshot_version", cart.version(),
                "cart_items", lines.stream().map(line -> Map.<String,Object>of("product_id", line.productId().toString(), "quantity", line.quantity())).toList(),
                "expected_products", expected, "requirements", cart.buildRequirements()));
        } catch (Exception ex) {
            throw new OrderException(HttpStatus.SERVICE_UNAVAILABLE, "PC_BUILD_CHECK_UNAVAILABLE", "Chưa kiểm tra được cấu hình. Không thể đặt hàng lúc này.");
        }
        try {
            if (!Boolean.TRUE.equals(response.get("allowed")) || !"ready".equals(response.get("status"))
                    || !cart.mergeId().toString().equals(String.valueOf(response.get("cart_id")))
                    || decimal(response.get("cart_snapshot_version")).longValueExact() != cart.version()
                    || !(response.get("blockers") instanceof List<?> blockers) || !blockers.isEmpty()) throw blocked();
            Map<?,?> current = (Map<?,?>) response.get("current_cart");
            Map<?,?> report = (Map<?,?>) current.get("compatibility");
            if (!Boolean.TRUE.equals(current.get("purchasable")) || !Boolean.TRUE.equals(report.get("complete"))
                    || !"compatible".equals(report.get("status"))
                    || !(report.get("missing_components") instanceof List<?> missing) || !missing.isEmpty()
                    || !(report.get("checks") instanceof List<?> checks) || checks.isEmpty()) throw blocked();
            for (Object raw : checks) {
                Map<?,?> check = (Map<?,?>) raw;
                if (!(check.get("required") instanceof Boolean)) throw blocked();
                if (Boolean.TRUE.equals(check.get("required")) && !"pass".equals(check.get("status"))) throw blocked();
            }
            Map<UUID,Line> byId = new HashMap<>();
            for (Line line : lines) byId.put(line.productId(), line);
            Set<UUID> seen = new HashSet<>();
            for (Object raw : (List<?>) current.get("selected_products")) {
                Map<?,?> item = (Map<?,?>) raw;
                UUID id = UUID.fromString(String.valueOf(item.get("product_id")));
                Line line = byId.get(id);
                if (line == null || !seen.add(id) || !Boolean.TRUE.equals(item.get("can_purchase"))
                        || decimal(item.get("quantity")).intValueExact() != line.quantity()
                        || decimal(item.get("unit_price_vnd")).compareTo(line.unitPrice()) != 0
                        || decimal(item.get("product_version")).longValueExact() != versions.get(id)) throw blocked();
            }
            if (!seen.equals(byId.keySet())) throw blocked();
        } catch (OrderException ex) { throw ex; }
        catch (RuntimeException ex) { throw blocked(); }
    }
    private static BigDecimal decimal(Object value) { return new BigDecimal(String.valueOf(value)); }
    private static OrderException blocked() {
        return OrderException.conflict("PC_BUILD_NOT_READY", "Cấu hình còn thiếu dữ liệu, linh kiện hoặc có điều kiện chưa đạt. Chưa thể đặt hàng.");
    }
}
