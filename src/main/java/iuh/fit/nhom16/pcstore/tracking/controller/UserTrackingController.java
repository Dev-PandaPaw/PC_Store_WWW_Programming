package iuh.fit.nhom16.pcstore.tracking.controller;

import iuh.fit.nhom16.pcstore.cart.security.CartUserIdentity;
import iuh.fit.nhom16.pcstore.tracking.dto.TrackingEventRequest;
import iuh.fit.nhom16.pcstore.tracking.entity.UserActivityLog;
import iuh.fit.nhom16.pcstore.tracking.service.UserTrackingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
public class UserTrackingController {

    private final UserTrackingService userTrackingService;

    public UserTrackingController(UserTrackingService userTrackingService) {
        this.userTrackingService = userTrackingService;
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> trackEvent(
            @Valid @RequestBody TrackingEventRequest request,
            HttpSession session,
            Authentication authentication
    ) {
        String sessionId = session != null ? session.getId() : "anonymous";
        UUID userId = resolveUserId(authentication);

        UserActivityLog saved = userTrackingService.logEvent(request, sessionId, userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "id", saved != null && saved.getId() != null ? saved.getId().toString() : "",
                "eventType", request.eventType()
        ));
    }

 
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestParam(defaultValue = "7") int days,
            HttpSession session,
            Authentication authentication
    ) {
        String sessionId = session != null ? session.getId() : "anonymous";
        UUID userId = resolveUserId(authentication);

        List<String> topCategories = userTrackingService.getTopCategories(sessionId, userId, days);
        List<UUID> topProducts = userTrackingService.getTopProducts(sessionId, userId, days);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "userId", userId != null ? userId.toString() : "",
                "topCategories", topCategories,
                "topProducts", topProducts
        ));
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (authentication.getPrincipal() instanceof CartUserIdentity identity) {
                return identity.userId();
            }
        }
        return null;
    }
}

