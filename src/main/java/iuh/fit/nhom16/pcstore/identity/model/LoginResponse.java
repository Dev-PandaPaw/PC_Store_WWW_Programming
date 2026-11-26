package iuh.fit.nhom16.pcstore.identity.model;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import java.util.List;
import java.util.UUID;

/** One committed response updates the authenticated account and merged cart together. */
public record LoginResponse(UUID id, String username, String fullName, String email, String phone,
                            List<String> roles, CartResponse cart) {
    public static LoginResponse of(TestUser user, CartResponse cart) {
        return new LoginResponse(user.id(), user.username(), user.fullName(), user.email(), user.phone(), user.roles(), cart);
    }
}
