package iuh.fit.nhom16.pcstore.cart.controller;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;
import java.util.UUID;

/** Development simulation only. Never activate cart-demo on public/shared production servers. */
@Profile("cart-demo")
@RestController
@RequestMapping("/api/v1/cart/demo")
public class CartDemoController {
    private final CartRepository repository;
    private final CartStorageService storage;
    private final CartService rules;
    public CartDemoController(CartRepository repository, CartStorageService storage, CartService rules) {
        this.repository = repository;
        this.storage = storage;
        this.rules = rules;
    }
    public record DemoOwner(UUID userId) { }

    @GetMapping
    public DemoOwner get(HttpServletRequest request) {
        return new DemoOwner((UUID) request.getSession().getAttribute(CartOwnerResolver.DEMO_OWNER));
    }

    @PostMapping
    public CartResponse select(@RequestBody DemoOwner owner, HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            throw new CartException(HttpStatus.CONFLICT, "CART_DEMO_AUTHENTICATED", "Phiên đã đăng nhập thật.");
        }
        var session = request.getSession();
        synchronized (WebUtils.getSessionMutex(session)) {
            if (owner.userId() != null && !repository.activeUser(owner.userId())) {
                throw new CartException(HttpStatus.NOT_FOUND, "CART_DEMO_USER_NOT_FOUND",
                        "Hãy chọn UUID của tài khoản ACTIVE đã có trong database.");
            }
            CartResponse result = owner.userId() == null
                    ? storage.execute(session, null, rules::get) : storage.merge(session, owner.userId());
            request.changeSessionId();
            if (owner.userId() == null) session.removeAttribute(CartOwnerResolver.DEMO_OWNER);
            else session.setAttribute(CartOwnerResolver.DEMO_OWNER, owner.userId());
            return result;
        }
    }
}
