package iuh.fit.nhom16.pcstore.cart.service;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.AddItemRequest;
import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.WebUtils;

import java.util.UUID;
import java.util.function.Function;

@Service
public class CartStorageService {
    private final CartRepository repository;
    private final TransactionTemplate transactions;
    private final CartService rules;

    public CartStorageService(CartRepository repository, TransactionTemplate transactions, CartService rules) {
        this.repository = repository;
        this.transactions = transactions;
        this.rules = rules;
    }

    public CartResponse execute(HttpSession session, UUID userId, Function<CartSession, CartResponse> operation) {
        synchronized (WebUtils.getSessionMutex(session)) {
            if (userId != null) {
                return transactions.execute(status -> {
                    CartSession cart = repository.lock(userId);
                    long version = cart.version();
                    CartResponse result = operation.apply(cart);
                    if (cart.version() != version || !cart.pendingAppliedAdds().isEmpty()) {
                        repository.save(cart);
                    }
                    return result;
                });
            }
            CartSession current = (CartSession) session.getAttribute(CartSession.ATTRIBUTE);
            CartSession cart = current == null ? new CartSession() : current.copy();
            CartResponse result = operation.apply(cart);
            cart.clearPendingAppliedAdds();
            session.setAttribute(CartSession.ATTRIBUTE, cart);
            return result;
        }
    }

    /**
     * Executes add-to-cart inside a DB transaction (holding product FOR SHARE locks until commit
     * for both user and guest carts) and preloads any persisted idempotency receipt for operationId.
     */
    public CartResponse executeAdd(HttpSession session, UUID userId, AddItemRequest request) {
        synchronized (WebUtils.getSessionMutex(session)) {
            if (userId != null) {
                return transactions.execute(status -> {
                    CartSession cart = repository.lock(userId, request.operationId());
                    long version = cart.version();
                    CartResponse result = rules.add(cart, request);
                    if (cart.version() != version || !cart.pendingAppliedAdds().isEmpty()) {
                        repository.save(cart);
                    }
                    return result;
                });
            }
            CartSession current = (CartSession) session.getAttribute(CartSession.ATTRIBUTE);
            CartSession copy = current == null ? new CartSession() : current.copy();
            CartResponse result = transactions.execute(status -> rules.add(copy, request));
            copy.clearPendingAppliedAdds();
            session.setAttribute(CartSession.ATTRIBUTE, copy);
            return result;
        }
    }

    /** Stable copy for advice; release locks before any remote request. */
    public CartSession snapshot(HttpSession session, UUID userId) {
        synchronized (WebUtils.getSessionMutex(session)) {
            if (userId != null) {
                return transactions.execute(status -> repository.lock(userId).copy());
            }
            CartSession current = (CartSession) session.getAttribute(CartSession.ATTRIBUTE);
            if (current == null) {
                current = new CartSession();
                session.setAttribute(CartSession.ATTRIBUTE, current);
            }
            return current.copy();
        }
    }

    /** Guest state is published only after the product-check transaction commits. */
    public CartResponse executeChecked(HttpSession session, UUID userId, Function<CartSession, CartResponse> operation) {
        if (userId != null) {
            return execute(session, userId, operation);
        }
        synchronized (WebUtils.getSessionMutex(session)) {
            CartSession current = (CartSession) session.getAttribute(CartSession.ATTRIBUTE);
            CartSession copy = current == null ? new CartSession() : current.copy();
            CartResponse result = transactions.execute(status -> operation.apply(copy));
            copy.clearPendingAppliedAdds();
            session.setAttribute(CartSession.ATTRIBUTE, copy);
            return result;
        }
    }

    public CartResponse merge(HttpSession session, UUID userId) {
        synchronized (WebUtils.getSessionMutex(session)) {
            if (userId == null) {
                return execute(session, null, rules::get);
            }
            CartSession guest = (CartSession) session.getAttribute(CartSession.ATTRIBUTE);
            if (guest == null || guest.quantities().isEmpty()) {
                return execute(session, userId, rules::get);
            }
            CartResponse result = transactions.execute(status -> {
                CartSession cart = repository.lock(userId);
                if (repository.claimMerge(guest.mergeId(), cart.mergeId())) {
                    // All-or-nothing: preserve guest cart if any combined line exceeds limits.
                    CartResponse merged = rules.merge(cart, guest);
                    repository.save(cart);
                    return merged;
                }
                return rules.get(cart);
            });
            session.removeAttribute(CartSession.ATTRIBUTE);
            return result;
        }
    }
}
