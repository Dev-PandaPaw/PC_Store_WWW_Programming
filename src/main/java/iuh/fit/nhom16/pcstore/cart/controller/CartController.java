package iuh.fit.nhom16.pcstore.cart.controller;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static iuh.fit.nhom16.pcstore.cart.dto.CartDtos.*;

@RestController
@Validated
@RequestMapping("/api/v1/cart")
public class CartController {
    private final CartService cartService;
    private final CartStorageService storage;
    private final CartOwnerResolver owners;

    public CartController(CartService cartService, CartStorageService storage, CartOwnerResolver owners) {
        this.cartService = cartService;
        this.storage = storage;
        this.owners = owners;
    }

    @GetMapping
    public CartResponse getCart(HttpSession session, Authentication authentication) {
        return storage.execute(session, owners.resolve(authentication, session), cartService::get);
    }

    @PostMapping("/items")
    public CartResponse addItem(HttpSession session,
                                Authentication authentication,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyHeader,
                                @Valid @RequestBody AddItemRequest request) {
        AddItemRequest resolved = resolveAddOperationId(request, idempotencyHeader);
        return storage.executeAdd(session, owners.resolve(authentication, session), resolved);
    }

    @PatchMapping("/items/{productId}")
    public CartResponse updateItem(HttpSession session, Authentication authentication, @PathVariable UUID productId,
                                   @Valid @RequestBody UpdateItemRequest request) {
        return storage.executeChecked(session, owners.resolve(authentication, session),
                cart -> cartService.update(cart, productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(HttpSession session, Authentication authentication, @PathVariable UUID productId,
                                   @RequestParam @NotNull @Min(0) Long cartVersion) {
        return storage.execute(session, owners.resolve(authentication, session),
                cart -> cartService.remove(cart, productId, cartVersion));
    }

    @DeleteMapping
    public CartResponse clearCart(HttpSession session, Authentication authentication,
                                  @RequestParam @NotNull @Min(0) Long cartVersion) {
        return storage.execute(session, owners.resolve(authentication, session),
                cart -> cartService.clear(cart, cartVersion));
    }

    @PostMapping("/merge")
    public CartResponse merge(HttpSession session, Authentication authentication) {
        return storage.merge(session, owners.resolve(authentication, session));
    }

    private AddItemRequest resolveAddOperationId(AddItemRequest request, String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return request;
        }
        UUID headerUuid;
        try {
            headerUuid = UUID.fromString(headerValue.trim());
        } catch (IllegalArgumentException ex) {
            throw new CartException(HttpStatus.BAD_REQUEST, "CART_INVALID_OPERATION_ID",
                    "Idempotency-Key phải là UUID hợp lệ.");
        }
        if (request.operationId() != null && !request.operationId().equals(headerUuid)) {
            throw new CartException(HttpStatus.BAD_REQUEST, "CART_INVALID_OPERATION_ID",
                    "Idempotency-Key không khớp với operationId trong dữ liệu gửi lên.");
        }
        return request.withOperationId(headerUuid);
    }
}
