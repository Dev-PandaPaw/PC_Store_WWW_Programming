package iuh.fit.nhom16.pcstore.sales;

import iuh.fit.nhom16.pcstore.identity.model.TestUser;
import iuh.fit.nhom16.pcstore.identity.service.TestLoginService;
import iuh.fit.nhom16.pcstore.sales.exception.OrderException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestLoginServiceTest {
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final TestLoginService service = new TestLoginService(jdbc, passwords);
    @Test void wrongCredentialsCannotProvisionOrAuthenticate() {
        assertEquals(401, assertThrows(OrderException.class, () -> service.login("test", "wrong")).status().value());
        assertThrows(OrderException.class, () -> service.login("someone", "1234"));
        verifyNoInteractions(jdbc, passwords);
    }
    @Test void fixedCredentialsProvisionOnlyDedicatedTestAccount() {
        when(passwords.encode("1234")).thenReturn("encoded-test-password");
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(TestUser.ID))).thenReturn(false, true);
        TestUser user = service.login("test", "1234");
        assertEquals(TestUser.ID, user.userId()); assertEquals("test", user.username());
        verify(jdbc).update(contains("ON CONFLICT (id) DO NOTHING"), eq(TestUser.ID), eq(user.email()), eq("encoded-test-password"), eq(user.phone()), eq(user.fullName()));
        verify(jdbc).update(contains("INSERT INTO user_roles"), eq(TestUser.ID), eq("ROLE_CUSTOMER"));
    }
    @Test void lockedTestAccountCannotLoginOrGainRoles() {
        when(passwords.encode(anyString())).thenReturn("encoded-test-password");
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(TestUser.ID))).thenReturn(false);
        assertEquals(403, assertThrows(OrderException.class, () -> service.login("test", "1234")).status().value());
        verify(jdbc, never()).update(contains("INSERT INTO user_roles"), any(), any());
    }
    @Test void returningAccountDoesNotRehashPasswordOrRewriteUserAndRoles() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq(TestUser.ID))).thenReturn(true);
        assertEquals(TestUser.ID, service.login("test", "1234").id());
        verifyNoInteractions(passwords);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
