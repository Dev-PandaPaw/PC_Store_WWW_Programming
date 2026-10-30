package iuh.fit.nhom16.pcstore.cart;

import iuh.fit.nhom16.pcstore.cart.exception.CartException;
import iuh.fit.nhom16.pcstore.cart.repository.CartRepository;
import iuh.fit.nhom16.pcstore.cart.security.CartOwnerResolver;
import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartOwnerResolverTest {
    @Test void normalStartupKeepsGuestAnonymousUntilLogin() {
        var repository = mock(CartRepository.class);
        var environment = new MockEnvironment();
        var session = new MockHttpSession();
        var resolver = new CartOwnerResolver(environment, repository);
        assertNull(resolver.resolve(null, session));
        verifyNoInteractions(repository);
    }

    @Test void oldOrderDemoProfileDoesNotAutoLogin() {
        var repository = mock(CartRepository.class);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("order-demo");
        var resolver = new CartOwnerResolver(environment, repository);
        assertNull(resolver.resolve(null, new MockHttpSession()));
        verifyNoInteractions(repository);
    }

    @Test void ignoresDemoIdentityUnlessExplicitProfileIsEnabled() {
        var repository = mock(CartRepository.class);
        var environment = new MockEnvironment();
        var session = new MockHttpSession();
        UUID id = UUID.randomUUID();
        session.setAttribute(CartOwnerResolver.DEMO_OWNER, id);
        var resolver = new CartOwnerResolver(environment, repository);
        assertNull(resolver.resolve(null, session));
        verifyNoInteractions(repository);
        environment.setActiveProfiles("cart-demo");
        when(repository.activeUser(id)).thenReturn(true);
        assertEquals(id, resolver.resolve(null, session));
    }

    @Test void realPrincipalTakesPrecedenceAndInactiveUserIsRejected() {
        var repository = mock(CartRepository.class);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("cart-demo");
        var resolver = new CartOwnerResolver(environment, repository);
        var session = new MockHttpSession();
        UUID id = UUID.randomUUID();
        session.setAttribute(CartOwnerResolver.DEMO_OWNER, UUID.randomUUID());
        CartUserIdentity principal = () -> id;
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(repository.activeUser(id)).thenReturn(true);
        assertEquals(id, resolver.resolve(auth, session));
        when(repository.activeUser(id)).thenReturn(false);
        assertThrows(CartException.class, () -> resolver.resolve(auth, session));
    }
}
