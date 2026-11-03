package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.identity.controller.TestLoginController;
import iuh.fit.nhom16.pcstore.identity.model.TestUser;
import iuh.fit.nhom16.pcstore.identity.service.TestLoginService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestLoginControllerTest {
    final TestLoginService service = mock(TestLoginService.class);
    final CartStorageService carts = mock(CartStorageService.class);
    final TestLoginController controller = new TestLoginController(service, carts);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void successfulLoginReturnsTheCommittedMergeInTheSameResponse() {
        TestUser user = TestUser.account();
        CartResponse merged = new CartResponse(8, List.of(), 5, BigDecimal.valueOf(500));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession(); request.setSession(session);
        when(service.login("test", "1234")).thenReturn(user);
        when(carts.merge(session, user.id())).thenReturn(merged);

        var response = controller.login(new TestLoginController.LoginRequest("test", "1234"), request, new MockHttpServletResponse());

        assertSame(merged, response.cart());
        assertEquals(user.id(), response.id());
        assertEquals(user, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertNotNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        verify(carts).merge(session, user.id()); verifyNoMoreInteractions(carts);
    }

    @Test void failedMergeDoesNotAuthenticateOrPublishASuccessfulCartSnapshot() {
        TestUser user = TestUser.account();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession(); request.setSession(session);
        when(service.login("test", "1234")).thenReturn(user);
        when(carts.merge(session, user.id())).thenThrow(new IllegalStateException("merge failed"));
        assertThrows(IllegalStateException.class, () -> controller.login(new TestLoginController.LoginRequest("test", "1234"), request, new MockHttpServletResponse()));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }
    @Test void secondLoginMergesIntoSecondAccountAndDoesNotGrantAdmin() {
        TestUser user = TestUser.customerAccount();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession(); request.setSession(session);
        CartResponse merged = new CartResponse(1, List.of(), 0, BigDecimal.ZERO);
        when(service.login("test2", "1234")).thenReturn(user);
        when(carts.merge(session, user.id())).thenReturn(merged);

        var response = controller.login(new TestLoginController.LoginRequest("test2", "1234"), request, new MockHttpServletResponse());

        assertEquals(TestUser.SECOND_ID, response.id());
        assertSame(merged, response.cart());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(List.of("ROLE_CUSTOMER"), auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        verify(carts).merge(session, TestUser.SECOND_ID);
        verify(carts, never()).merge(session, TestUser.ID);
    }
}
