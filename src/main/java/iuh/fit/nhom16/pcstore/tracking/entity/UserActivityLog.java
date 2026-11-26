package iuh.fit.nhom16.pcstore.tracking.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "user_activity_logs")
public class UserActivityLog {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "category_slug", length = 100)
    private String categorySlug;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Ho_Chi_Minh")
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(VN_ZONE);

    public UserActivityLog() {
    }

    public UserActivityLog(UUID userId, String sessionId, String eventType, UUID productId, String categorySlug, String metadata) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.productId = productId;
        this.categorySlug = categorySlug;
        this.metadata = metadata;
        this.createdAt = OffsetDateTime.now(VN_ZONE);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getCategorySlug() {
        return categorySlug;
    }

    public void setCategorySlug(String categorySlug) {
        this.categorySlug = categorySlug;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

