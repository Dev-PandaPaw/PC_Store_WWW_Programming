package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PcBuildCartApplyTest {
    final UUID old = UUID.randomUUID(), added = UUID.randomUUID();
    final ProductRepository products = mock(ProductRepository.class);
    final CartService carts = new CartService(products);
    CartProductSnapshot product(UUID id, int stock) {
        return new CartProductSnapshot(id, "TEST", "Synthetic", new BigDecimal("10"), "ACTIVE", stock, 0);
    }
    @Test void replacingAllLinesChangesVersionOnlyOnce() {
        CartSession cart = new CartSession(UUID.randomUUID(),7,Map.of(old,1));
        when(products.findCartProducts(any())).thenReturn(List.of(product(added,5)));
        var result = carts.applyProposal(cart,Map.of(added,2),7);
        assertEquals(Map.of(added,2),cart.quantities());
        assertEquals(8,result.cartVersion());
        assertEquals(new BigDecimal("20"),result.subtotal());
    }
    @Test void badLaterLineCannotPartiallyRemoveOrAddItems() {
        CartSession cart = new CartSession(UUID.randomUUID(),7,Map.of(old,1));
        when(products.findCartProducts(any())).thenReturn(List.of(product(old,5),product(added,0)));
        assertThrows(CartException.class,() -> carts.applyProposal(cart,Map.of(old,2,added,1),7));
        assertEquals(Map.of(old,1),cart.quantities());
        assertEquals(7,cart.version());
    }
    @Test void sameTargetMarksBuildPurposeOnceAndPreservesIdempotenceAfterThat() {
        CartSession cart = new CartSession(UUID.randomUUID(),7,Map.of(old,1));
        when(products.findCartProducts(any())).thenReturn(List.of(product(old,5)));
        carts.applyProposal(cart,Map.of(old,1),7);
        assertEquals(8,cart.version());
        assertTrue(cart.buildCheckoutRequired());
        assertTrue(cart.copy().buildCheckoutRequired());
        carts.applyProposal(cart,Map.of(old,1),8);
        assertEquals(8,cart.version());
    }
    @Test void staleVersionIsRejectedBeforeProductQuery() {
        var cart = new CartSession(UUID.randomUUID(),7,Map.of(old,1));
        assertThrows(CartException.class,() -> carts.applyProposal(cart,Map.of(added,1),6));
        verifyNoInteractions(products);
    }
    @Test void guestStateIsNotPublishedWhenDatabaseCommitFails() {
        var repository = mock(CartRepository.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doThrow(new IllegalStateException("commit failed")).when(manager).commit(any());
        var storage = new CartStorageService(repository,new TransactionTemplate(manager),carts);
        var session = new MockHttpSession();
        session.setAttribute(CartSession.ATTRIBUTE,new CartSession(UUID.randomUUID(),7,Map.of(old,1)));
        when(products.findCartProducts(any())).thenReturn(List.of(product(added,5)));
        assertThrows(IllegalStateException.class,() -> storage.executeChecked(session,null,
                cart -> carts.applyProposal(cart,Map.of(added,1),7)));
        var unchanged = (CartSession) session.getAttribute(CartSession.ATTRIBUTE);
        assertEquals(Map.of(old,1),unchanged.quantities());
        assertEquals(7,unchanged.version());
        verifyNoInteractions(repository);
    }
    @Test void authenticatedCartSaveIsTransactionalAndScopedToOwner() {
        UUID owner = UUID.randomUUID();
        var repository = mock(CartRepository.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(repository.lock(owner)).thenReturn(new CartSession(UUID.randomUUID(),7,Map.of(old,1)));
        when(products.findCartProducts(any())).thenReturn(List.of(product(added,5)));
        var storage = new CartStorageService(repository,new TransactionTemplate(manager),carts);
        storage.executeChecked(new MockHttpSession(),owner,cart -> carts.applyProposal(cart,Map.of(added,1),7));
        var order = inOrder(repository,manager);
        order.verify(repository).lock(owner);
        order.verify(repository).save(any());
        order.verify(manager).commit(any());
    }
}
