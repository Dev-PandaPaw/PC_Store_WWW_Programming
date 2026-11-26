package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.AddItemRequest;
import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.UpdateItemRequest;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot;
import iuh.fit.nhom16.pcstore.catalog.domain.Product;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final CartSession session = new CartSession();
    private CartService cartService;
    private final Map<UUID, Product> fixtures = new HashMap<>();

    @BeforeEach
    void setUp() {
        cartService = new CartService(products);
        when(products.findCartProducts(any())).thenAnswer(call ->
                ((Collection<UUID>) call.getArgument(0)).stream()
                        .map(fixtures::get).filter(Objects::nonNull)
                        .map(this::snapshot).toList());
        when(products.lockCartProducts(any())).thenAnswer(call ->
                ((Collection<UUID>) call.getArgument(0)).stream()
                        .map(fixtures::get).filter(Objects::nonNull)
                        .map(this::snapshot).toList());
    }

    @Test
    void addingProductLocksAndReadsOnlyOneSnapshotBatchIncludingExistingLines() {
        Product cpu = product("CPU", 8);
        Product gpu = product("GPU", 8);
        CartSession cart = new CartSession(UUID.randomUUID(), 2, Map.of(cpu.getId(), 2));

        var result = cartService.add(cart, new AddItemRequest(gpu.getId(), 3, 2L));

        assertEquals(5, result.totalQuantity());
        assertEquals(BigDecimal.valueOf(5_000_000), result.subtotal());
        verify(products).lockCartProducts(Set.of(cpu.getId(), gpu.getId()));
        verifyNoMoreInteractions(products);
    }

    @Test
    void readsAllCartProductsInOneBatchAndPreservesMissingLines() {
        Product cpu = product("CPU", 8);
        UUID missing = UUID.randomUUID();
        CartSession cart = new CartSession(UUID.randomUUID(), 3, Map.of(cpu.getId(), 2, missing, 1));
        doReturn(List.of(snapshot(cpu))).when(products).findCartProducts(any());

        var response = cartService.get(cart);

        assertEquals(2, response.items().size());
        assertEquals(3, response.totalQuantity());
        assertEquals(BigDecimal.valueOf(2_000_000), response.subtotal());
        assertFalse(response.items().stream().filter(item -> item.productId().equals(missing)).findFirst().orElseThrow().purchasable());
        verify(products).findCartProducts(cart.quantities().keySet());
        verify(products, never()).findById(any());
    }

    @Test
    void unchangedQuantityKeepsVersionAndInvalidQuantityKeepsCart() {
        Product cpu = product("CPU", 8);
        cartService.add(session, new AddItemRequest(cpu.getId(), 2, 0L));

        assertEquals(1, cartService.update(session, cpu.getId(), new UpdateItemRequest(2, 1L)).cartVersion());
        assertThrows(CartException.class, () -> cartService.update(session, cpu.getId(), new UpdateItemRequest(9, 1L)));
        assertEquals(2, session.quantities().get(cpu.getId()));
        assertEquals(1, session.version());
    }

    @Test
    void mergeBuildsOnlyOneResponse() {
        Product cpu = product("CPU", 8);
        Product gpu = product("GPU", 8);
        CartSession guest = new CartSession(UUID.randomUUID(), 0, Map.of(cpu.getId(), 2, gpu.getId(), 3));

        assertEquals(5, cartService.merge(session, guest).totalQuantity());
        verify(products, times(1)).lockCartProducts(any());
    }

    @Test
    void sameSkuIsAccumulatedWithoutChangingInventory() {
        Product product = product("CPU-1", 8);

        var first = cartService.add(session, new AddItemRequest(product.getId(), 1, 0L));
        var second = cartService.add(session, new AddItemRequest(product.getId(), 2, first.cartVersion()));

        assertEquals(1, second.items().size());
        assertEquals(3, second.items().getFirst().quantity());
        assertEquals(8, product.getStockOnHand());
        assertEquals(0, product.getReservedQuantity());
    }

    @Test
    void differentSkusAreSeparateCartLines() {
        Product cpu = product("CPU-1", 8);
        Product gpu = product("GPU-1", 5);

        var first = cartService.add(session, new AddItemRequest(cpu.getId(), 1, 0L));
        var second = cartService.add(session, new AddItemRequest(gpu.getId(), 1, first.cartVersion()));

        assertEquals(2, second.items().size());
        assertEquals(2, second.totalQuantity());
    }

    @Test
    void rejectsInactiveAndInsufficientStockWithExactRemainingAddableQuantity() {
        Product inactive = product("OLD-1", 8);
        inactive.setStatus("INACTIVE");
        CartException unavailable = assertThrows(CartException.class,
                () -> cartService.add(session, new AddItemRequest(inactive.getId(), 1, 0L)));
        assertEquals("CART_PRODUCT_UNAVAILABLE", unavailable.getErrorCode());

        Product lowStock = product("RAM-1", 5);
        var added = cartService.add(session, new AddItemRequest(lowStock.getId(), 3, 0L));
        CartException insufficient = assertThrows(CartException.class,
                () -> cartService.add(session, new AddItemRequest(lowStock.getId(), 3, added.cartVersion())));
        assertEquals("CART_INSUFFICIENT_STOCK", insufficient.getErrorCode());
        assertEquals(5, insufficient.getDetails().get("availableQuantity"));
        assertEquals(3, insufficient.getDetails().get("currentCartQuantity"));
        assertEquals(2, insufficient.getDetails().get("remainingAddableQuantity"));
        assertTrue(insufficient.getMessage().contains("2"));
    }

    @Test
    void idempotentAddPreventsDoubleCountingEvenAfterCartReloadWithNewVersion() {
        Product gpu = product("GPU-IDEM", 10);
        UUID opId = UUID.randomUUID();

        // First request succeeds on backend (version 0 -> 1, quantity -> 2), suppose response is lost
        var first = cartService.add(session, new AddItemRequest(gpu.getId(), 2, 0L, opId));
        assertEquals(2, first.totalQuantity());
        assertEquals(1L, first.cartVersion());

        // Case 1: direct retry with old cartVersion (0) and same operationId
        var retrySameVersion = cartService.add(session, new AddItemRequest(gpu.getId(), 2, 0L, opId));
        assertEquals(2, retrySameVersion.totalQuantity());
        assertEquals(1L, retrySameVersion.cartVersion());

        // Case 2: client reloads cart (gets cartVersion = 1) and resends add with NEW version (1) + same operationId
        var retryNewVersion = cartService.add(session, new AddItemRequest(gpu.getId(), 2, 1L, opId));
        assertEquals(2, retryNewVersion.totalQuantity());
        assertEquals(1L, retryNewVersion.cartVersion());

        // Case 3: reusing same operationId with different quantity is rejected with 409 CART_IDEMPOTENCY_CONFLICT
        CartException conflict = assertThrows(CartException.class,
                () -> cartService.add(session, new AddItemRequest(gpu.getId(), 3, 1L, opId)));
        assertEquals("CART_IDEMPOTENCY_CONFLICT", conflict.getErrorCode());
    }

    @Test
    void rejectsAStaleCartVersionWhenOperationIdIsDifferent() {
        Product product = product("SSD-1", 8);
        cartService.add(session, new AddItemRequest(product.getId(), 1, 0L, UUID.randomUUID()));

        CartException error = assertThrows(CartException.class,
                () -> cartService.add(session, new AddItemRequest(product.getId(), 1, 0L, UUID.randomUUID())));

        assertEquals("CART_VERSION_CONFLICT", error.getErrorCode());
    }

    private CartProductSnapshot snapshot(Product product) {
        return new CartProductSnapshot(product.getId(), product.getSku(), product.getName(), product.getPrice(),
                product.getStatus(), product.getStockOnHand(), product.getReservedQuantity());
    }

    private Product product(String sku, int stock) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSku(sku);
        product.setName(sku + " product");
        product.setPrice(BigDecimal.valueOf(1_000_000));
        product.setStatus("ACTIVE");
        product.setStockOnHand(stock);
        product.setReservedQuantity(0);
        fixtures.put(product.getId(), product);
        return product;
    }
}
