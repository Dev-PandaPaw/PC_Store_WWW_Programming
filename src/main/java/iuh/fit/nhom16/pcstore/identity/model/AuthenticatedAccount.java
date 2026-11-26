package iuh.fit.nhom16.pcstore.identity.model;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/** Session identity shared by development authentication, cart and checkout. */
public record AuthenticatedAccount(UUID userId, String email, Long sessionVersion) implements CartUserIdentity, Principal, Serializable {
    public AuthenticatedAccount(UUID userId, String email) {
        this(userId, email, null);
    }

    @Override public String getName() { return email; }
}
