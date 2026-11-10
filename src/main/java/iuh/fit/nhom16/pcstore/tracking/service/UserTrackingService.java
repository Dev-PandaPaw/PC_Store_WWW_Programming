package iuh.fit.nhom16.pcstore.tracking.service;

import iuh.fit.nhom16.pcstore.tracking.dto.TrackingEventRequest;
import iuh.fit.nhom16.pcstore.tracking.entity.UserActivityLog;
import iuh.fit.nhom16.pcstore.tracking.repository.UserActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.data.domain.PageRequest;

@Service
public class UserTrackingService {

    private static final Logger log = LoggerFactory.getLogger(UserTrackingService.class);

    private final UserActivityLogRepository activityLogRepository;

    public UserTrackingService(UserActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getTopViewedCategories(UUID userId, Instant since, int limit) {
        if (userId==null || since==null || limit<1 || limit>10)
            throw new IllegalArgumentException("Invalid recommendation history query");
        return activityLogRepository.findTopViewedCategoriesByUser(userId,
                OffsetDateTime.ofInstant(since,ZoneOffset.UTC),PageRequest.of(0,limit));
    }

    @Transactional
    public UserActivityLog logEvent(TrackingEventRequest request, String sessionId, UUID userId) {
        if (request == null || request.eventType() == null || request.eventType().isBlank()) {
            return null;
        }

        UserActivityLog activityLog = new UserActivityLog(
                userId,
                sessionId,
                request.eventType().trim().toUpperCase(),
                request.productId(),
                request.categorySlug() != null ? request.categorySlug().trim().toLowerCase() : null,
                request.metadata()
        );

        UserActivityLog saved = activityLogRepository.save(activityLog);
        log.debug("Recorded tracking event: type={}, session={}, user={}, product={}",
                saved.getEventType(), saved.getSessionId(), saved.getUserId(), saved.getProductId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<String> getTopCategories(String sessionId, UUID userId, int days) {
        int windowDays = days > 0 ? days : 7;
        OffsetDateTime since = OffsetDateTime.now(UserActivityLog.VN_ZONE).minusDays(windowDays);
        return activityLogRepository.findTopCategoriesBySessionOrUser(sessionId, userId, since);
    }

    @Transactional(readOnly = true)
    public List<UUID> getTopProducts(String sessionId, UUID userId, int days) {
        int windowDays = days > 0 ? days : 7;
        OffsetDateTime since = OffsetDateTime.now(UserActivityLog.VN_ZONE).minusDays(windowDays);
        return activityLogRepository.findTopProductIdsBySessionOrUser(sessionId, userId, since);
    }

    @Transactional(readOnly = true)
    public List<UserActivityLog> getRecentActivities(String sessionId, UUID userId) {
        return activityLogRepository.findTop20BySessionIdOrUserIdOrderByCreatedAtDesc(sessionId, userId);
    }
}

