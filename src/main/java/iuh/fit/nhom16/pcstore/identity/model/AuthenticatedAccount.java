package iuh.fit.nhom16.pcstore.identity.model;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/** Session identity shared by development authentication, cart and checkout. */
public record AuthenticatedAccount(UUID userId, String email) implements CartUserIdentity, Principal, Serializable {
    @Override public String getName() { return email; }
}
