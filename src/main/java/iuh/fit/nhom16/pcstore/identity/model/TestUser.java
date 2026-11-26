package iuh.fit.nhom16.pcstore.identity.model;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/** Development accounts; each identity is stored in its own authenticated session. */
public record TestUser(UUID id, String username, String fullName, String email, String phone,
                       List<String> roles) implements CartUserIdentity, Serializable {
    public static final UUID ID = UUID.fromString("a0000000-0000-0000-0000-000000000005");
    public static final String USERNAME = "test";
    public static final String PASSWORD = "1234";
    public static final UUID SECOND_ID = UUID.fromString("a0000000-0000-0000-0000-000000000006");
    public static final String SECOND_USERNAME = "test2";
    public static TestUser account() {
        return new TestUser(ID, USERNAME, "Tài khoản kiểm thử", "test@pcstore.example", "+84987654321", List.of("CUSTOMER", "ADMIN"));
    }
    public static TestUser customerAccount() {
        return new TestUser(SECOND_ID, SECOND_USERNAME, "Khách hàng kiểm thử 2", "test2@pcstore.example", "+84976543210", List.of("CUSTOMER"));
    }
    public static List<TestUser> accounts() {
        return List.of(account(), customerAccount());
    }
    @Override public UUID userId() { return id; }
}
