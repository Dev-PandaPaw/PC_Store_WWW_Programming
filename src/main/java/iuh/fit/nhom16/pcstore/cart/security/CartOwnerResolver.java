package iuh.fit.nhom16.pcstore.cart.security;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CartOwnerResolver {
    public static final String DEMO_OWNER = "cart.demo.user";
    private final Environment environment;
    private final CartRepository repository;
    public CartOwnerResolver(Environment environment, CartRepository repository) {
        this.environment = environment;
        this.repository = repository;
    }

    public UUID resolve(Authentication authentication, HttpSession session) {
        UUID userId = null;
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (!(authentication.getPrincipal() instanceof CartUserIdentity identity)) {
                throw new CartException(HttpStatus.UNAUTHORIZED, "CART_IDENTITY_REQUIRED",
                        "Chưa tích hợp định danh tài khoản cho giỏ hàng.");
            }
            userId = identity.userId();
            if (userId == null) throw new CartException(HttpStatus.UNAUTHORIZED,
                    "CART_IDENTITY_REQUIRED", "Không xác định được tài khoản.");
        } else if (environment.acceptsProfiles(Profiles.of("cart-demo"))) {
            userId = (UUID) session.getAttribute(DEMO_OWNER);
        }
        if (userId != null && !repository.activeUser(userId)) {
            throw new CartException(HttpStatus.UNAUTHORIZED, "CART_USER_UNAVAILABLE", "Tài khoản không khả dụng.");
        }
        return userId;
    }
}
