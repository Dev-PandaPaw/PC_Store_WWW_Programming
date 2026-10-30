package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.AddItemRequest;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.catalog.Product;
import iuh.fit.nhom16.pcstore.catalog.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartStorageServiceTest {
    final CartRepository repository = mock(CartRepository.class);
    final ProductRepository products = mock(ProductRepository.class);
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final CartService rules = new CartService(products);
    final CartStorageService storage = new CartStorageService(repository, new TransactionTemplate(manager), rules);
    final UUID owner = UUID.randomUUID();
    final UUID productId = UUID.randomUUID();
    CartSession persisted = new CartSession();

    @BeforeEach void setup() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.lock(owner)).thenAnswer(call -> persisted.copy());
        doAnswer(call -> { persisted = ((CartSession) call.getArgument(0)).copy(); return null; })
                .when(repository).save(any());
        when(repository.claimMerge(any(), any())).thenReturn(true);
        Product product = new Product();
        product.setId(productId);
        product.setName("SKU");
        product.setSku("SKU");
        product.setPrice(BigDecimal.TEN);
        product.setStockOnHand(10);
        product.setReservedQuantity(2);
        when(products.findCartProducts(any())).thenReturn(java.util.List.of(
                new iuh.fit.nhom16.pcstore.catalog.CartProductSnapshot(productId, product.getSku(), product.getName(),
                        product.getPrice(), product.getStatus(), product.getStockOnHand(), product.getReservedQuantity())));
    }

    @Test void guestCartIsSessionOnlyAndIndependent() {
        MockHttpSession one = new MockHttpSession();
        storage.execute(one, null, cart -> rules.add(cart, new AddItemRequest(productId, 2, 0L)));
        assertEquals(2, storage.execute(one, null, rules::get).totalQuantity());
        assertEquals(0, storage.execute(new MockHttpSession(), null, rules::get).totalQuantity());
        verifyNoInteractions(repository, manager);
    }

    @Test void userCartSurvivesNewSessionAndGuestCannotReadIt() {
        storage.execute(new MockHttpSession(), owner, cart -> rules.add(cart, new AddItemRequest(productId, 2, 0L)));
        assertEquals(2, storage.execute(new MockHttpSession(), owner, rules::get).totalQuantity());
        assertEquals(0, storage.execute(new MockHttpSession(), null, rules::get).totalQuantity());
        UUID other = UUID.randomUUID();
        when(repository.lock(other)).thenReturn(new CartSession());
        assertEquals(0, storage.execute(new MockHttpSession(), other, rules::get).totalQuantity());
    }

    @Test void mergesSameSkuAndClearsGuestOnlyAfterCommit() {
        MockHttpSession guest = new MockHttpSession();
        storage.execute(guest, null, cart -> rules.add(cart, new AddItemRequest(productId, 2, 0L)));
        storage.execute(new MockHttpSession(), owner, cart -> rules.add(cart, new AddItemRequest(productId, 3, 0L)));
        assertEquals(5, storage.merge(guest, owner).totalQuantity());
        assertNull(guest.getAttribute(CartSession.ATTRIBUTE));
        assertEquals(5, storage.merge(guest, owner).totalQuantity());
        verify(repository, times(1)).claimMerge(any(), any());
    }

    @Test void failedMergeRollsBackAndPreservesGuest() {
        MockHttpSession guest = new MockHttpSession();
        storage.execute(guest, null, cart -> rules.add(cart, new AddItemRequest(productId, 5, 0L)));
        persisted = new CartSession(UUID.randomUUID(), 1, Map.of(productId, 5));
        CartException error = assertThrows(CartException.class, () -> storage.merge(guest, owner));
        assertEquals("CART_INSUFFICIENT_STOCK", error.getErrorCode());
        assertEquals(5, ((CartSession) guest.getAttribute(CartSession.ATTRIBUTE)).quantities().get(productId));
        assertEquals(5, persisted.quantities().get(productId));
        verify(repository, never()).save(any());
        verify(manager).rollback(any());
    }

    @Test void retryAfterCommittedMergeDoesNotDoubleCount() {
        MockHttpSession guest = new MockHttpSession();
        storage.execute(guest, null, cart -> rules.add(cart, new AddItemRequest(productId, 2, 0L)));
        persisted = new CartSession(UUID.randomUUID(), 1, Map.of(productId, 2));
        when(repository.claimMerge(any(), any())).thenReturn(false);
        assertEquals(2, storage.merge(guest, owner).totalQuantity());
        verify(repository, never()).save(any());
        assertNull(guest.getAttribute(CartSession.ATTRIBUTE));
    }

    @Test void staleUserVersionDoesNotOverwritePersistedCart() {
        persisted = new CartSession(UUID.randomUUID(), 7, Map.of(productId, 2));
        assertThrows(CartException.class, () -> storage.execute(new MockHttpSession(), owner,
                cart -> rules.add(cart, new AddItemRequest(productId, 1, 0L))));
        verify(repository, never()).save(any());
        assertEquals(2, persisted.quantities().get(productId));
    }

    @Test void failedGuestMutationKeepsOriginalState() {
        MockHttpSession guest = new MockHttpSession();
        storage.execute(guest, null, cart -> rules.add(cart, new AddItemRequest(productId, 2, 0L)));
        assertThrows(CartException.class, () -> storage.execute(guest, null,
                cart -> rules.add(cart, new AddItemRequest(productId, 7, 1L))));
        assertEquals(2, storage.execute(guest, null, rules::get).totalQuantity());
    }
}
