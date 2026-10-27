package iuh.fit.nhom16.pcstore.identity.service;

import iuh.fit.nhom16.pcstore.identity.model.TestUser;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name="app.test-login.enabled", havingValue="true", matchIfMissing=true)
public class TestLoginService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    public TestLoginService(JdbcTemplate jdbc, PasswordEncoder passwords) { this.jdbc = jdbc; this.passwords = passwords; }

    @Transactional
    public TestUser login(String username, String password) {
        if (!TestUser.USERNAME.equals(username) || !TestUser.PASSWORD.equals(password))
            throw new OrderException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng.");
        TestUser user = TestUser.account();
        // Returning users need one status read, not another BCrypt calculation and three no-op inserts.
        if (active(user)) return user;
        // Provision only this new test account; never overwrite an existing customer's profile or verification.
        jdbc.update("""
                INSERT INTO users(id,email,password_hash,phone,full_name,status,phone_verified_at)
                VALUES (?,?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING
                """, user.id(), user.email(), passwords.encode(TestUser.PASSWORD), user.phone(), user.fullName());
        if (!active(user)) throw new OrderException(HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE", "Tài khoản kiểm thử đã bị khóa.");
        for (String role : user.roles()) jdbc.update("INSERT INTO user_roles(user_id,role) VALUES (?,?) ON CONFLICT DO NOTHING", user.id(), "ROLE_" + role);
        return user;
    }
    private boolean active(TestUser user) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM users WHERE id=? AND status='ACTIVE')", Boolean.class, user.id()));
    }
}
