package iuh.fit.nhom16.pcstore.identity.controller;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.identity.model.TestUser;
import iuh.fit.nhom16.pcstore.identity.model.LoginResponse;
import iuh.fit.nhom16.pcstore.identity.service.TestLoginService;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name="app.test-login.enabled", havingValue="true")
public class TestLoginController {
    private final TestLoginService login;
    private final CartStorageService carts;
    private final HttpSessionSecurityContextRepository contexts = new HttpSessionSecurityContextRepository();
    public TestLoginController(TestLoginService login, CartStorageService carts) { this.login = login; this.carts = carts; }
    public record LoginRequest(@NotBlank @Size(max=100) String username, @NotBlank @Size(max=100) String password) { }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        TestUser user = login.login(body.username(), body.password());
        var session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            // Merge succeeds before authenticating. A failed merge leaves the guest cart available for retry.
            var cart = carts.merge(session, user.id());
            request.changeSessionId();
            session.removeAttribute(CartOwnerResolver.DEMO_OWNER);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null,
                    user.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            return LoginResponse.of(user, cart);
        }
    }
    @GetMapping("/me")
    public TestUser me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof TestUser user))
            throw new OrderException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "Bạn chưa đăng nhập.");
        return user;
    }
    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(CartSession.ATTRIBUTE);
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        contexts.saveContext(SecurityContextHolder.createEmptyContext(), request, response);
    }
}
