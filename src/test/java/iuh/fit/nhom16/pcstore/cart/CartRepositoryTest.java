package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationEntity;
import iuh.fit.nhom16.pcstore.cart.entity.CartAddOperationId;
import iuh.fit.nhom16.pcstore.cart.entity.CartEntity;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartAddOperationJpaRepository;
import iuh.fit.nhom16.pcstore.cart.repository.CartJpaRepository;
import iuh.fit.nhom16.pcstore.cart.repository.CartMergeReceiptJpaRepository;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartRepositoryTest {
    private final CartJpaRepository carts = mock(CartJpaRepository.class);
    private final CartMergeReceiptJpaRepository mergeReceipts = mock(CartMergeReceiptJpaRepository.class);
    private final CartAddOperationJpaRepository addOperations = mock(CartAddOperationJpaRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CartRepository repository = new CartRepository(carts, mergeReceipts, addOperations, users);

    @Test
    void lockEnsuresCartRowLocksWithPessimisticWriteAndPreloadsOperationReceipt() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID opId = UUID.randomUUID();

        CartEntity entity = new CartEntity(cartId, userId);
        entity.setVersion(3L);
        entity.syncItems(Map.of(productId, 2));

        when(carts.findByUserIdForUpdate(userId)).thenReturn(Optional.of(entity));
        when(addOperations.findById(new CartAddOperationId(cartId, opId)))
                .thenReturn(Optional.of(new CartAddOperationEntity(cartId, opId, productId, 2, 3L)));

        CartSession locked = repository.lock(userId, opId);

        verify(carts, never()).ensureCartExists(any(UUID.class), any(UUID.class));
        verify(carts).findByUserIdForUpdate(userId);
        assertEquals(cartId, locked.mergeId());
        assertEquals(3L, locked.version());
        assertEquals(Map.of(productId, 2), locked.quantities());
        assertNotNull(locked.appliedAdd(opId));
        assertEquals(2, locked.appliedAdd(opId).quantity());
    }

    @Test
    void missingCartIsCreatedThenLocked() {
        UUID userId = UUID.randomUUID();
        CartEntity entity = new CartEntity(UUID.randomUUID(), userId);
        when(carts.findByUserIdForUpdate(userId)).thenReturn(Optional.empty(), Optional.of(entity));

        assertEquals(entity.getId(), repository.lock(userId).mergeId());

        verify(carts).ensureCartExists(any(UUID.class), eq(userId));
        verify(carts, times(2)).findByUserIdForUpdate(userId);
    }

    @Test
    void saveUpdatesEntityItemsInPlaceWithoutRecreatingKeptProductAndPersistsAddOperations() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID keptProduct = UUID.randomUUID();
        UUID removedProduct = UUID.randomUUID();
        UUID addedProduct = UUID.randomUUID();
        UUID opId = UUID.randomUUID();

        CartEntity entity = new CartEntity(cartId, userId);
        entity.syncItems(Map.of(keptProduct, 1, removedProduct, 2));
        UUID originalKeptLineId = entity.getItems().stream()
                .filter(i -> i.getProductId().equals(keptProduct))
                .findFirst().orElseThrow().getLineId();

        when(carts.findById(cartId)).thenReturn(Optional.of(entity));
        when(addOperations.existsById(new CartAddOperationId(cartId, opId))).thenReturn(false);

        CartSession session = new CartSession(cartId, 4L, Map.of(keptProduct, 3, addedProduct, 1));
        session.recordAppliedAdd(new CartSession.AppliedAddOperation(opId, addedProduct, 1, 4L));

        repository.save(session);

        assertEquals(2, entity.getItems().size());
        var keptItem = entity.getItems().stream()
                .filter(i -> i.getProductId().equals(keptProduct))
                .findFirst().orElseThrow();
        assertEquals(originalKeptLineId, keptItem.getLineId());
        assertEquals(3, keptItem.getQuantity());
        assertEquals(4L, entity.getVersion());

        verify(carts).saveAndFlush(entity);
        verify(addOperations).save(any(CartAddOperationEntity.class));
        verify(addOperations).flush();
        assertTrue(session.pendingAppliedAdds().isEmpty());
    }

    @Test
    void clearingCartRemovesOrphanItemsViaJpaAndChecksActiveUserAndMergeReceipts() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        CartEntity entity = new CartEntity(cartId, userId);
        entity.syncItems(Map.of(UUID.randomUUID(), 2));
        when(carts.findById(cartId)).thenReturn(Optional.of(entity));

        repository.save(new CartSession(cartId, 5L, Map.of()));
        assertTrue(entity.getItems().isEmpty());
        verify(carts).saveAndFlush(entity);

        when(mergeReceipts.claimIfAbsent(guestId, cartId)).thenReturn(1);
        assertTrue(repository.claimMerge(guestId, cartId));

        when(users.existsByIdAndStatusIgnoreCase(userId, "ACTIVE")).thenReturn(true);
        assertTrue(repository.activeUser(userId));
    }
}
