package iuh.fit.nhom16.pcstore.tracking;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import iuh.fit.nhom16.pcstore.tracking.controller.UserTrackingController;
import iuh.fit.nhom16.pcstore.tracking.dto.TrackingEventRequest;
import iuh.fit.nhom16.pcstore.tracking.entity.UserActivityLog;
import iuh.fit.nhom16.pcstore.tracking.repository.UserActivityLogRepository;
import iuh.fit.nhom16.pcstore.tracking.service.UserTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserTrackingTest {

    private UserActivityLogRepository repository;
    private UserTrackingService service;
    private UserTrackingController controller;

    @BeforeEach
    void setUp() {
        repository = mock(UserActivityLogRepository.class);
        service = new UserTrackingService(repository);
        controller = new UserTrackingController(service);
    }

    @Test
    @DisplayName("Should successfully log guest user tracking event")
    void shouldLogGuestTrackingEvent() {
        UUID productId = UUID.randomUUID();
        TrackingEventRequest request = new TrackingEventRequest(
                "view_product",
                productId,
                "CPU",
                "{\"price\": 4000000}",
                "2026-11-09T16:00:00Z"
        );

        when(repository.save(any(UserActivityLog.class))).thenAnswer(invocation -> {
            UserActivityLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        MockHttpSession session = new MockHttpSession();
        session.changeSessionId();

        ResponseEntity<Map<String, Object>> response = controller.trackEvent(request, session, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("success", response.getBody().get("status"));
        assertEquals("view_product", response.getBody().get("eventType"));

        ArgumentCaptor<UserActivityLog> captor = ArgumentCaptor.forClass(UserActivityLog.class);
        verify(repository).save(captor.capture());

        UserActivityLog saved = captor.getValue();
        assertEquals("VIEW_PRODUCT", saved.getEventType());
        assertEquals("cpu", saved.getCategorySlug());
        assertEquals(productId, saved.getProductId());
        assertEquals(session.getId(), saved.getSessionId());
        assertNull(saved.getUserId());
    }

    @Test
    @DisplayName("Should associate authenticated user ID with tracking event")
    void shouldAssociateAuthenticatedUserId() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        TrackingEventRequest request = new TrackingEventRequest(
                "DWELL_TIME_REACHED",
                productId,
                "vga",
                "{\"dwellSeconds\": 30}",
                "2026-11-09T16:00:00Z"
        );

        when(repository.save(any(UserActivityLog.class))).thenAnswer(invocation -> {
            UserActivityLog log = invocation.getArgument(0);
            log.setId(UUID.randomUUID());
            return log;
        });

        CartUserIdentity identity = () -> userId;
        var auth = new UsernamePasswordAuthenticationToken(identity, null, List.of());
        MockHttpSession session = new MockHttpSession();

        ResponseEntity<Map<String, Object>> response = controller.trackEvent(request, session, auth);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        ArgumentCaptor<UserActivityLog> captor = ArgumentCaptor.forClass(UserActivityLog.class);
        verify(repository).save(captor.capture());

        UserActivityLog saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("DWELL_TIME_REACHED", saved.getEventType());
    }

    @Test
    @DisplayName("Should return user profile with top categories and top products")
    void shouldReturnUserProfile() {
        MockHttpSession session = new MockHttpSession();
        String sessionId = session.getId();

        when(repository.findTopCategoriesBySessionOrUser(eq(sessionId), isNull(), any(OffsetDateTime.class)))
                .thenReturn(List.of("cpu", "vga", "ram"));
        when(repository.findTopProductIdsBySessionOrUser(eq(sessionId), isNull(), any(OffsetDateTime.class)))
                .thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));

        ResponseEntity<Map<String, Object>> response = controller.getUserProfile(7, session, null);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(List.of("cpu", "vga", "ram"), body.get("topCategories"));
        assertEquals(2, ((List<?>) body.get("topProducts")).size());
    }
}

