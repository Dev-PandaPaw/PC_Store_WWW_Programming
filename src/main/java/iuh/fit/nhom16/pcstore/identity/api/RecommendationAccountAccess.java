package iuh.fit.nhom16.pcstore.identity.api;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class RecommendationAccountAccess {
    private final UserRepository users;
    public RecommendationAccountAccess(UserRepository users) { this.users=users; }
    public UUID requireActiveUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof CartUserIdentity identity) || identity.userId()==null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Bạn cần đăng nhập bằng tài khoản cửa hàng.");
        var account=users.findById(identity.userId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!account.isActive()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản không khả dụng.");
        return identity.userId();
    }
}
