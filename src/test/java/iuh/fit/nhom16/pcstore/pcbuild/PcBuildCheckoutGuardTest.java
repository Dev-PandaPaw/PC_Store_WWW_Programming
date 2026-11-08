package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.sales.dto.OrderDtos.Line;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PcBuildCheckoutGuardTest {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final PcBuildClient python = mock(PcBuildClient.class);
    final PcBuildCheckoutGuard guard = new PcBuildCheckoutGuard(jdbc, python);
    final UUID product = UUID.randomUUID();
    final CartSession cart = new CartSession(UUID.randomUUID(), 4, Map.of(product, 1));
    final Line line = new Line(product, "Synthetic CPU", "CPU-TEST", new BigDecimal("1000"), 1, 36);
    @BeforeEach void setup() {
        when(jdbc.queryForMap(anyString(), eq(product))).thenReturn(Map.of("price", line.unitPrice(), "version", 7L));
        when(python.checkout(anyMap())).thenReturn(ready());
    }
    Map<String,Object> ready() {
        return new HashMap<>(Map.of("allowed", true, "status", "ready", "blockers", List.of(),
            "cart_id", cart.mergeId().toString(), "cart_snapshot_version", cart.version(),
            "current_cart", Map.of("purchasable", true, "compatibility", Map.of("status", "compatible", "complete", true,
                "missing_components", List.of(), "checks", List.of(Map.of("required", true, "status", "pass"))),
                "selected_products", List.of(Map.of("product_id", product.toString(), "can_purchase", true,
                    "quantity", 1, "unit_price_vnd", "1000", "product_version", 7)))));
    }
    @Test void ordinaryRetailCartDoesNotRequireBuildGate() {
        guard.requireReady(cart, List.of(line)); verifyNoInteractions(jdbc, python);
    }
    @Test void markedCartUsesFreshPricesVersionsAndServerOnlyCheckoutEndpoint() {
        cart.markBuildCheckout(); guard.requireReady(cart, List.of(line));
        verify(jdbc).execute("LOCK TABLE product_component_specs, pc_build_rules IN SHARE MODE");
        verify(python).checkout(argThat(body -> body.containsKey("expected_products") && body.get("cart_id").equals(cart.mergeId().toString())));
    }
    @Test void unknownCannotBeOverriddenByAcknowledgementOrAllowedFlag() {
        cart.markBuildCheckout();
        var response = ready();
        response.put("current_cart", Map.of("purchasable", true, "compatibility", Map.of("status", "needs_verification", "complete", true)));
        when(python.checkout(anyMap())).thenReturn(response);
        assertEquals("PC_BUILD_NOT_READY", assertThrows(OrderException.class, () -> guard.requireReady(cart, List.of(line))).code());
    }
    @Test void changedPriceBlocksBeforeRemoteCheck() {
        cart.markBuildCheckout();
        when(jdbc.queryForMap(anyString(), eq(product))).thenReturn(Map.of("price", new BigDecimal("1500"), "version", 8L));
        assertEquals("PC_BUILD_PRICE_CHANGED", assertThrows(OrderException.class, () -> guard.requireReady(cart, List.of(line))).code());
        verifyNoInteractions(python);
    }
    @Test void serviceFailureBlocksOrderInsteadOfFailingOpen() {
        cart.markBuildCheckout(); when(python.checkout(anyMap())).thenThrow(new RuntimeException("private"));
        var error = assertThrows(OrderException.class, () -> guard.requireReady(cart, List.of(line)));
        assertEquals("PC_BUILD_CHECK_UNAVAILABLE", error.code()); assertFalse(error.getMessage().contains("private"));
    }
    @Test void staleOrDifferentCartResponseIsRejected() {
        cart.markBuildCheckout(); var response = ready(); response.put("cart_snapshot_version", 3);
        when(python.checkout(anyMap())).thenReturn(response);
        assertThrows(OrderException.class, () -> guard.requireReady(cart, List.of(line)));
    }
    @Test void missingSelectedProductsIsRejected() {
        cart.markBuildCheckout(); var response = ready();
        response.put("current_cart", Map.of("purchasable", true, "compatibility", Map.of("status", "compatible", "complete", true,
            "missing_components", List.of(), "checks", List.of(Map.of("required", true, "status", "pass"))), "selected_products", List.of()));
        when(python.checkout(anyMap())).thenReturn(response);
        assertThrows(OrderException.class, () -> guard.requireReady(cart, List.of(line)));
    }
    @Test void originalRequirementsSurviveCopyMergeAndReachTheCheckoutGate() {
        cart.setBuildRequirements(Map.of("require_dedicated_gpu", true, "min_ram_gb", 64));
        assertEquals(64, cart.copy().buildRequirements().get("min_ram_gb"));
        cart.mergeBuildRequirements(Map.of("min_ram_gb", 32, "min_storage_gb", 2000));
        assertEquals(64, cart.buildRequirements().get("min_ram_gb"));
        guard.requireReady(cart, List.of(line));
        verify(python).checkout(argThat(body -> ((Map<?,?>)body.get("requirements")).get("min_ram_gb").equals(64)));
    }
    @Test void buildMarkerSurvivesCopyAndCanBeClearedOnlyAsNewCartIntent() {
        cart.markBuildCheckout(); assertTrue(cart.copy().buildCheckoutRequired());
        cart.clearBuildCheckout(); assertFalse(cart.buildCheckoutRequired());
    }
}
