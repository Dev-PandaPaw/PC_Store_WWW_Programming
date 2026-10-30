package iuh.fit.nhom16.pcstore.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_challenges")
public class VerificationChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String purpose; // PASSWORD_RESET, EMAIL_VERIFY

    @Column(nullable = false, length = 255)
    private String target; // email

    @Column(name = "secret_digest", nullable = false, length = 255)
    private String secretDigest; // SHA-256 hash của mã OTP

    @Column(name = "session_binding", length = 255)
    private String sessionBinding;

    @Column(name = "auth_version")
    private Long authVersion;

    @Column(nullable = false)
    private short attempts = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resend_after")
    private Instant resendAfter;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VerificationChallenge() {
    }

    public VerificationChallenge(UUID userId, String purpose, String target, String secretDigest, Instant expiresAt, Instant resendAfter) {
        this.userId = userId;
        this.purpose = purpose;
        this.target = target;
        this.secretDigest = secretDigest;
        this.expiresAt = expiresAt;
        this.resendAfter = resendAfter;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean canResend() {
        return resendAfter == null || Instant.now().isAfter(resendAfter);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    // Getters and Setters
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getSecretDigest() {
        return secretDigest;
    }

    public void setSecretDigest(String secretDigest) {
        this.secretDigest = secretDigest;
    }

    public String getSessionBinding() {
        return sessionBinding;
    }

    public void setSessionBinding(String sessionBinding) {
        this.sessionBinding = sessionBinding;
    }

    public Long getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(Long authVersion) {
        this.authVersion = authVersion;
    }

    public short getAttempts() {
        return attempts;
    }

    public void setAttempts(short attempts) {
        this.attempts = attempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResendAfter() {
        return resendAfter;
    }

    public void setResendAfter(Instant resendAfter) {
        this.resendAfter = resendAfter;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(Instant consumedAt) {
        this.consumedAt = consumedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
