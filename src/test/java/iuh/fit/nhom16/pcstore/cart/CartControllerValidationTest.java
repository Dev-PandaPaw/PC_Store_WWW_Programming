package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.controller.CartController;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.catalog.api.dto.CartProductSnapshot;
import iuh.fit.nhom16.pcstore.catalog.infrastructure.persistence.ProductRepository;
import iuh.fit.nhom16.pcstore.configuration.web.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerValidationTest {
    private final UUID productId = UUID.randomUUID();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CartRepository cartRepository = mock(CartRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        List<CartProductSnapshot> snapshots = List.of(
                new CartProductSnapshot(productId, "GPU-5070", "RTX 5070", BigDecimal.valueOf(18_000_000), "ACTIVE", 5, 0));
        when(productRepository.findCartProducts(any())).thenReturn(snapshots);
        when(productRepository.lockCartProducts(any())).thenReturn(snapshots);

        CartService cartService = new CartService(productRepository);
        CartStorageService storage = new CartStorageService(cartRepository, new TransactionTemplate(txManager), cartService);
        CartOwnerResolver owners = new CartOwnerResolver(new MockEnvironment(), cartRepository);
        mvc = MockMvcBuilders.standaloneSetup(new CartController(cartService, storage, owners))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void rejectsDecimalAndStringQuantitiesWith400WithoutCoercingToInteger() throws Exception {
        MockHttpSession session = new MockHttpSession();
        for (String rawQuantity : List.of("1.5", "2.0", "0.99", "\"2\"", "\"1.5\"", "true")) {
            mvc.perform(post("/api/v1/cart/items")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":\"" + productId + "\",\"quantity\":" + rawQuantity + ",\"cartVersion\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("CART_INVALID_QUANTITY"));
        }

        // Valid integer succeeds and starts at version 1 (confirming none of the decimals mutated state)
        mvc.perform(post("/api/v1/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":2,\"cartVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.cartVersion").value(1));

        // Update endpoint also rejects decimal quantities with 400
        mvc.perform(patch("/api/v1/cart/items/" + productId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3.5,\"cartVersion\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CART_INVALID_QUANTITY"));
    }

    @Test
    void returnsRemainingAddableQuantityInProblemDetailAndSupportsIdempotencyHeader() throws Exception {
        MockHttpSession session = new MockHttpSession();
        UUID opId = UUID.randomUUID();

        mvc.perform(post("/api/v1/cart/items")
                        .session(session)
                        .header("Idempotency-Key", opId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":3,\"cartVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.cartVersion").value(1));

        // Retry with NEW cartVersion=1 and same Idempotency-Key does not double-add
        mvc.perform(post("/api/v1/cart/items")
                        .session(session)
                        .header("Idempotency-Key", opId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":3,\"cartVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.cartVersion").value(1));

        // Exceeding stock (5 available, 3 in cart -> remainingAddableQuantity = 2)
        mvc.perform(post("/api/v1/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"" + productId + "\",\"quantity\":4,\"cartVersion\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CART_INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.availableQuantity").value(5))
                .andExpect(jsonPath("$.currentCartQuantity").value(3))
                .andExpect(jsonPath("$.remainingAddableQuantity").value(2));
    }
}
