package iuh.fit.nhom16.pcstore.sales.security;

import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Account;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class OrderActorResolver {
    private final CartOwnerResolver owners;
    private final JdbcTemplate jdbc;
    public OrderActorResolver(CartOwnerResolver owners, JdbcTemplate jdbc) { this.owners = owners; this.jdbc = jdbc; }
    public UUID resolve(Authentication authentication, HttpSession session) {
        UUID id = owners.resolve(authentication, session);
        if (id == null) throw new OrderException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "Vui lòng đăng nhập trước khi đặt hàng.");
        return id;
    }
    // Recheck status and roles in the transaction, including when the session is still valid.
    public Account require(UUID id, String role) {
        var accounts = jdbc.query("SELECT * FROM users WHERE id = ? AND status = 'ACTIVE' FOR SHARE",
                (rs, n) -> new Account(id, rs.getString("full_name"), rs.getString("email"), rs.getString("phone"),
                        rs.getTimestamp("phone_verified_at") != null,
                        jdbc.queryForList("SELECT replace(role, 'ROLE_', '') FROM user_roles WHERE user_id = ?", String.class, id)), id);
        if (accounts.isEmpty()) throw new OrderException(HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE", "Tài khoản không khả dụng.");
        Account account = accounts.getFirst();
        if (!account.roles().contains(role)) throw new OrderException(HttpStatus.FORBIDDEN, "ROLE_REQUIRED", "Không có quyền thực hiện thao tác.");
        return account;
    }
}
