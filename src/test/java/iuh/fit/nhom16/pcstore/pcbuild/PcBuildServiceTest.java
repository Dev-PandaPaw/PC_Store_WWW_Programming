package iuh.fit.nhom16.pcstore.pcbuild;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import iuh.fit.nhom16.pcstore.cart.model.CartSession;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.cart.service.CartService;
import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import static iuh.fit.nhom16.pcstore.pcbuild.PcBuildDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PcBuildServiceTest {
    final UUID owner = UUID.randomUUID(), cpu = UUID.randomUUID(), gpu = UUID.randomUUID();
    final MockHttpSession session = new MockHttpSession();
    final CartStorageService storage = mock(CartStorageService.class);
    final CartService carts = mock(CartService.class);
    final PcBuildClient python = mock(PcBuildClient.class);
    final PcBuildInventory inventory = mock(PcBuildInventory.class);
    final PcBuildService service = new PcBuildService(storage, carts, python, inventory);
    CartSession current = new CartSession(UUID.randomUUID(), 3, Map.of(cpu, 1));
    @BeforeEach void setup() {
        when(storage.snapshot(any(), any())).thenAnswer(call -> current.copy());
        when(python.post(eq("/api/pc-build/advice"), any())).thenAnswer(call -> quote(true));
        when(python.post(eq("/api/internal/cart/check"), any())).thenReturn(
                Map.of("purchasable", true, "compatibility", Map.of("status", "needs_verification")));
        when(storage.executeChecked(any(), any(), any())).thenAnswer(call -> {
            CartSession next = current.copy();
            CartResponse response = ((Function<CartSession,CartResponse>) call.getArgument(2)).apply(next);
            current = next;
            return response;
        });
        when(carts.applyProposal(any(), any(), anyLong())).thenAnswer(call -> {
            CartSession cart = call.getArgument(0);
            cart.quantities().clear(); cart.quantities().putAll(call.getArgument(1)); cart.incrementVersion();
            return new CartResponse(cart.version(), List.of(), 2, new BigDecimal("30"));
        });
    }
    AdviceInput input(List<UUID> replace) { return new AdviceInput("Synthetic test", new BigDecimal("50"), true, false, replace); }
    Map<String,Object> item(UUID id, String price) {
        return Map.of("product_id", id.toString(), "name", "Synthetic", "quantity", 1,
                "unit_price_vnd", price, "line_total_vnd", price, "product_version", 4);
    }
    Map<String,Object> quote(boolean keepCpu) {
        var option = Map.of("items", keepCpu ? List.of(item(cpu,"10"), item(gpu,"20")) : List.of(item(gpu,"20")),
                "subtotal_vnd", keepCpu ? "30" : "20", "compatibility", Map.of("status", "needs_verification", "complete", true));
        return new HashMap<>(Map.of("cart_id", current.mergeId().toString(), "cart_snapshot_version", current.version(),
                "build_search", Map.of("recommendations", List.of(), "provisional_builds", List.of(option))));
    }
    UUID propose() { return service.advice(session, owner, input(List.of())).proposals().getFirst().proposalId(); }
    @Test void readingAdviceNeverChangesCartAndBuildsSnapshotOnServer() {
        var view = service.advice(session, owner, input(List.of()));
        assertEquals(1, view.proposals().size());
        assertEquals(Map.of(cpu, 1), current.quantities());
        verify(storage, never()).executeChecked(any(), any(), any());
        verify(python).post(eq("/api/pc-build/advice"), argThat(body -> {
            assertEquals(current.mergeId().toString(), body.get("cart_id"));
            assertEquals(3L, body.get("cart_snapshot_version"));
            assertFalse(body.containsKey("user_id"));
            return true;
        }));
    }
    @Test void confirmationAppliesOnceAfterFreshCheckAndLockedInventory() {
        UUID id = propose();
        service.confirm(session, owner, new ConfirmInput(id, true, true));
        assertEquals(Map.of(cpu,1,gpu,1), current.quantities());
        var order = inOrder(python, storage, inventory, carts);
        order.verify(python).post(eq("/api/internal/cart/check"), any());
        order.verify(storage).executeChecked(any(), eq(owner), any());
        order.verify(inventory).validateLocked(any());
        order.verify(carts).applyProposal(any(), any(), eq(3L));
        assertThrows(CartException.class, () -> service.confirm(session, owner, new ConfirmInput(id, true, true)));
        verify(carts, times(1)).applyProposal(any(), any(), anyLong());
    }
    @Test void cannotUseOtherSessionOrOtherOwnersProposal() {
        UUID id = propose();
        for (var scenario : List.of(new Object[]{new MockHttpSession(), owner}, new Object[]{session, UUID.randomUUID()}))
            assertThrows(CartException.class, () -> service.confirm((MockHttpSession)scenario[0], (UUID)scenario[1], new ConfirmInput(id,true,true)));
        verifyNoInteractions(inventory);
    }
    @Test void stageARequiresConfirmationButDoesNotRequireUnknownAcknowledgement() {
        UUID id = propose();
        assertEquals("PC_BUILD_CONFIRMATION_REQUIRED", assertThrows(CartException.class,
                () -> service.confirm(session, owner, new ConfirmInput(id,false,true))).getErrorCode());
        assertDoesNotThrow(() -> service.confirm(session, owner, new ConfirmInput(id,true,false)));
        verify(storage).executeChecked(any(), any(), any());
    }
    @Test void staleCartCannotBeOverwritten() {
        UUID id = propose(); current.incrementVersion();
        assertEquals("CART_VERSION_CONFLICT", assertThrows(CartException.class,
                () -> service.confirm(session, owner, new ConfirmInput(id,true,true))).getErrorCode());
        verifyNoInteractions(inventory);
    }
    @Test void concurrentCartChangeDuringRemoteRecheckIsDetectedInsideTransaction() {
        UUID id = propose();
        when(python.post(eq("/api/internal/cart/check"), any())).thenAnswer(call -> {
            current.incrementVersion();
            return Map.of("purchasable", true, "compatibility", Map.of("status", "compatible"));
        });
        assertThrows(CartException.class, () -> service.confirm(session, owner, new ConfirmInput(id,true,true)));
        verifyNoInteractions(inventory);
        verifyNoInteractions(carts);
    }
    @Test void changedInventoryLeavesAllOriginalLinesInPlace() {
        UUID id = propose();
        doThrow(PcBuildInventory.conflict("PC_BUILD_PRICE_CHANGED", "changed")).when(inventory).validateLocked(any());
        assertThrows(CartException.class, () -> service.confirm(session, owner, new ConfirmInput(id,true,true)));
        assertEquals(Map.of(cpu,1), current.quantities());
        verifyNoInteractions(carts);
    }
    @Test void failedCompatibilityRecheckCannotBeApplied() {
        UUID id = propose();
        for (String status : List.of("incompatible", "invalid")) {
            when(python.post(eq("/api/internal/cart/check"), any())).thenReturn(
                    Map.of("purchasable", true, "compatibility", Map.of("status", status)));
            assertThrows(CartException.class, () -> service.confirm(session, owner, new ConfirmInput(id,true,true)));
        }
        verify(storage, never()).executeChecked(any(), any(), any());
    }
    @Test void replacementsMustBeExplicitAndBelongToCurrentCart() {
        assertThrows(CartException.class, () -> service.advice(session, owner, input(List.of(gpu))));
        verifyNoInteractions(python);
        when(python.post(eq("/api/pc-build/advice"), any())).thenReturn(quote(false));
        var view = service.advice(session, owner, input(List.of(cpu)));
        assertEquals(List.of(cpu), view.proposals().getFirst().removedProductIds());
        assertEquals(Map.of(cpu,1), current.quantities());
        service.confirm(session, owner, new ConfirmInput(view.proposals().getFirst().proposalId(),true,true));
        assertEquals(Map.of(gpu,1), current.quantities());
    }
    @Test void cannotDropUnselectedKeptItemFromResponse() {
        when(python.post(eq("/api/pc-build/advice"), any())).thenReturn(quote(false));
        assertEquals("PC_BUILD_INVALID_RESPONSE", assertThrows(CartException.class,
                () -> service.advice(session, owner, input(List.of()))).getErrorCode());
    }
    @Test void badCorrelationAndBudgetAreRejected() {
        var response = quote(true); response.put("cart_id", UUID.randomUUID().toString());
        when(python.post(eq("/api/pc-build/advice"), any())).thenReturn(response);
        assertThrows(CartException.class, () -> service.advice(session, owner, input(List.of())));
        when(python.post(eq("/api/pc-build/advice"), any())).thenReturn(quote(true));
        assertThrows(CartException.class, () -> service.advice(session, owner,
                new AdviceInput("test", new BigDecimal("25"),true,false,List.of())));
    }
    @Test void expiredProposalIsRejectedEvenIfCartHasNotChanged() {
        var proposal = new PcBuildService.Proposal(owner,current.mergeId(),current.version(),Map.copyOf(current.quantities()),
                List.of(),Map.of(cpu,1),Instant.now().minusSeconds(1));
        assertEquals("PC_BUILD_PROPOSAL_EXPIRED", assertThrows(CartException.class,
                () -> PcBuildService.validateCart(proposal,current)).getErrorCode());
    }
}
