package iuh.fit.nhom16.pcstore.cart.repository;

import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationId;
import iuh.fit.nhom16.pcstore.cart.entity.CartEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartItemEntity;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class CartRepository {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CartJpaRepository carts;
    private final CartMergeReceiptJpaRepository mergeReceipts;
    private final CartAddOperationJpaRepository addOperations;
    private final UserRepository users;

    public CartRepository(CartJpaRepository carts,
                          CartMergeReceiptJpaRepository mergeReceipts,
                          CartAddOperationJpaRepository addOperations,
                          UserRepository users) {
        this.carts = carts;
        this.mergeReceipts = mergeReceipts;
        this.addOperations = addOperations;
        this.users = users;
    }

    public CartSession lock(UUID userId) {
        return lock(userId, null);
    }

    public CartSession lock(UUID userId, UUID operationId) {
        CartEntity entity = carts.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    carts.ensureCartExists(UUID.randomUUID(), userId);
                    return carts.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> new IllegalStateException("Cart row missing after ensureCartExists for user " + userId));
                });

        CartSession session = toSession(entity);

        if (operationId != null) {
            addOperations.findById(new CartAddOperationId(entity.getId(), operationId))
                    .ifPresent(op -> session.preloadAppliedAdd(new CartSession.AppliedAddOperation(
                            op.getOperationId(), op.getProductId(), op.getQuantity(), op.getResultVersion())));
        }
        return session;
    }

    /** One snapshot query; reading a cart must not wait for an in-flight writer's row lock. */
    public CartSession read(UUID userId) {
        return carts.findSnapshotByUserId(userId).map(this::toSession).orElseGet(CartSession::new);
    }

    private CartSession toSession(CartEntity entity) {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CartItemEntity item : entity.getItems()) {
            quantities.put(item.getProductId(), item.getQuantity());
        }

        return new CartSession(
                entity.getId(),
                entity.getVersion(),
                quantities,
                entity.isPcBuildCheckout(),
                readRequirements(entity.getPcBuildRequirements()));

    }

    public void save(CartSession cart) {
        CartEntity entity = carts.findById(cart.mergeId())
                .orElseThrow(() -> new IllegalStateException("Cart entity not found: " + cart.mergeId()));
        entity.syncItems(cart.quantities());
        entity.setVersion(cart.version());
        entity.setPcBuildCheckout(cart.buildCheckoutRequired());
        entity.setPcBuildRequirements(writeRequirements(cart.buildRequirements()));
        entity.setUpdatedAt(OffsetDateTime.now());
        carts.save(entity);

        if (!cart.pendingAppliedAdds().isEmpty()) {
            for (CartSession.AppliedAddOperation op : cart.pendingAppliedAdds()) {
                // Flush cart changes before the FK insert, without exists + merge SELECT round trips.
                addOperations.insertIfAbsent(cart.mergeId(), op.operationId(), op.productId(), op.quantity(), op.resultVersion());
            }
            cart.clearPendingAppliedAdds();
        }
    }

    public boolean claimMerge(UUID guestId, UUID cartId) {
        return mergeReceipts.claimIfAbsent(guestId, cartId) == 1;
    }

    public boolean activeUser(UUID userId) {
        return users.existsByIdAndStatusIgnoreCase(userId, "ACTIVE");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readRequirements(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        return JSON.readValue(value, Map.class);
    }

    private static String writeRequirements(Map<String, Object> requirements) {
        return JSON.writeValueAsString(requirements == null ? Map.of() : requirements);
    }
}
