package iuh.fit.nhom16.pcstore.identity.model;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/** The single development account; its identity is stored in the authenticated session. */
public record TestUser(UUID id, String username, String fullName, String email, String phone,
                       List<String> roles) implements CartUserIdentity, Serializable {
    public static final UUID ID = UUID.fromString("a0000000-0000-0000-0000-000000000005");
    public static final String USERNAME = "test";
    public static final String PASSWORD = "1234";
    public static TestUser account() {
        return new TestUser(ID, USERNAME, "Tài khoản kiểm thử", "test@pcstore.example", "+84987654321", List.of("CUSTOMER", "ADMIN"));
    }
    @Override public UUID userId() { return id; }
}
