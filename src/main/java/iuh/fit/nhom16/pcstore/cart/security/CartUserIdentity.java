package iuh.fit.nhom16.pcstore.cart.security;

import java.util.UUID;

/** Implement on the real authentication principal when the auth module is integrated. */
public interface CartUserIdentity {
    UUID userId();
}
