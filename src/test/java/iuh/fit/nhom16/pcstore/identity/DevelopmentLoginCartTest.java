package iuh.fit.nhom16.pcstore.identity;

import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.identity.application.AuthService;
import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.web.AuthController;
import iuh.fit.nhom16.pcstore.identity.web.dto.LoginRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DevelopmentLoginCartTest {
    final AuthService auth = mock(AuthService.class);
    final UserRepository users = mock(UserRepository.class);
    final CartStorageService carts = mock(CartStorageService.class);
    final AuthController controller = new AuthController(auth, users, carts);
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void realLoginReturnsMergedCartAndIdentityAcceptedByCheckout() {
        var user = new User("customer@example.test", "hash", "Customer", "0912345678");
        user.setId(UUID.randomUUID());
        var session = new MockHttpSession();
        session.setAttribute(CartOwnerResolver.DEMO_OWNER, UUID.randomUUID());
        String previousId = session.getId();
        var request = new MockHttpServletRequest(); request.setSession(session);
        var cart = new CartResponse(7, List.of(), 0, BigDecimal.ZERO);
        when(auth.authenticate(user.getEmail(), "password")).thenReturn(user);
        when(carts.merge(session, user.getId())).thenReturn(cart);

        var result = controller.login(new LoginRequest(user.getEmail(), "password"), request).getBody();
        assertNotNull(result);
        assertSame(cart, result.cart());
        assertEquals(user.getId(), result.id());
        assertNotEquals(previousId, session.getId());
        assertNull(session.getAttribute(CartOwnerResolver.DEMO_OWNER));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertInstanceOf(CartUserIdentity.class, authentication.getPrincipal());
        assertEquals(user.getEmail(), authentication.getName());
        var repository = mock(CartRepository.class);
        when(repository.activeUser(user.getId())).thenReturn(true);
        assertEquals(user.getId(), new CartOwnerResolver(new MockEnvironment(), repository).resolve(authentication, session));
        verify(carts).merge(session, user.getId());
    }

    @Test void failedMergeDoesNotAuthenticateOrRotateSession() {
        var user = new User("customer@example.test", "hash", "Customer", "0912345678");
        user.setId(UUID.randomUUID());
        var session = new MockHttpSession();
        String previousId = session.getId();
        var request = new MockHttpServletRequest(); request.setSession(session);
        when(auth.authenticate(user.getEmail(), "password")).thenReturn(user);
        when(carts.merge(session, user.getId())).thenThrow(new IllegalStateException("merge failed"));
        assertThrows(IllegalStateException.class, () -> controller.login(new LoginRequest(user.getEmail(), "password"), request));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(previousId, session.getId());
    }
}
