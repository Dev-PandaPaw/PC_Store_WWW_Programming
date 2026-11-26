package iuh.fit.nhom16.pcstore.identity.application;

import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.domain.VerificationChallenge;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.infrastructure.VerificationChallengeRepository;
import iuh.fit.nhom16.pcstore.identity.model.AuthenticatedAccount;
import iuh.fit.nhom16.pcstore.identity.web.dto.ChangePasswordRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.RegisterRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.ResetPasswordRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.UpdateProfileRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";
    private static final String PURPOSE_PHONE_VERIFY = "PHONE_VERIFY";
    private static final Pattern VN_PHONE_PATTERN = Pattern.compile("^(?:\\+84|0)[35789][0-9]{8}$");

    private final UserRepository userRepository;
    private final VerificationChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       VerificationChallengeRepository challengeRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public UserDto register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được sử dụng. Vui lòng chọn email khác.");
        }

        String phone = normalizePhoneOrNull(req.phone());
        if (phone != null && isPhoneTaken(phone, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng. Vui lòng chọn số khác.");
        }

        String encodedPassword = passwordEncoder.encode(req.password());
        User user = new User(email, encodedPassword, req.fullName().trim(), phone);
        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public User authenticate(String emailOrUsername, String rawPassword) {
        String target = emailOrUsername.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(target)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User requireAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }

        User user;
        if (auth.getPrincipal() instanceof AuthenticatedAccount account && account.userId() != null) {
            user = userRepository.findById(account.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại"));
            if (account.sessionVersion() != null && !Objects.equals(account.sessionVersion(), user.getSessionVersion())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hiệu lực. Vui lòng đăng nhập lại.");
            }
        } else {
            user = userRepository.findByEmailIgnoreCase(auth.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại"));
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa");
        }

        return user;
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        String fullName = req.fullName().trim();
        if (fullName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ và tên không được để trống.");
        }

        String normalizedNewPhone = normalizePhoneOrNull(req.phone());
        if (normalizedNewPhone != null && !VN_PHONE_PATTERN.matcher(normalizedNewPhone).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không đúng định dạng Việt Nam (ví dụ: 0912345678).");
        }

        if (normalizedNewPhone != null && isPhoneTaken(normalizedNewPhone, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Số điện thoại này đã được sử dụng bởi tài khoản khác.");
        }

        boolean samePhone = sameCanonicalPhone(user.getPhone(), normalizedNewPhone);
        user.setFullName(fullName);
        user.setPhone(normalizedNewPhone);
        if (!samePhone) {
            user.setPhoneVerifiedAt(null);
        }

        user.setRecipientName(trimToNull(req.recipientName(), 150));
        user.setProvince(trimToNull(req.province(), 100));
        user.setDistrict(trimToNull(req.district(), 100));
        user.setWard(trimToNull(req.ward(), 100));
        user.setAddressLine(trimToNull(req.addressLine(), 255));

        return UserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public User changePassword(UUID userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác.");
        }

        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.incrementSessionVersion();
        return userRepository.save(user);
    }

    @Transactional
    public void sendPhoneVerificationOtp(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        String phone = normalizePhoneOrNull(user.getPhone());
        if (phone == null || !VN_PHONE_PATTERN.matcher(phone).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng lưu số điện thoại hợp lệ trong hồ sơ trước khi xác minh.");
        }

        if (user.getPhoneVerifiedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại hiện tại đã được xác minh.");
        }

        String canonicalTarget = toPlus84Phone(phone);
        Optional<VerificationChallenge> latestOpt = challengeRepository
                .findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(canonicalTarget, PURPOSE_PHONE_VERIFY);

        if (latestOpt.isPresent() && !latestOpt.get().canResend()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng đợi 60 giây trước khi gửi lại mã OTP.");
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String secretDigest = sha256Hex(otpCode);

        Instant expiresAt = Instant.now().plus(5, ChronoUnit.MINUTES);
        Instant resendAfter = Instant.now().plus(60, ChronoUnit.SECONDS);

        VerificationChallenge challenge = new VerificationChallenge(
                user.getId(),
                PURPOSE_PHONE_VERIFY,
                canonicalTarget,
                secretDigest,
                expiresAt,
                resendAfter
        );
        challengeRepository.save(challenge);

        emailService.sendPhoneVerificationOtpEmail(user.getEmail(), phone, otpCode);
    }

    @Transactional
    public UserDto verifyPhoneWithOtp(UUID userId, String otpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại."));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        String phone = normalizePhoneOrNull(user.getPhone());
        if (phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản chưa có số điện thoại để xác minh.");
        }

        String canonicalTarget = toPlus84Phone(phone);
        VerificationChallenge challenge = challengeRepository
                .findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(canonicalTarget, PURPOSE_PHONE_VERIFY)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác minh không tồn tại hoặc đã hết hạn."));

        validateAndConsumeChallenge(challenge, otpCode);

        user.setPhoneVerifiedAt(Instant.now());
        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }

    @Transactional
    public void initiateForgotPassword(String email) {
        String target = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(target);

        // Chống Account Enumeration: Dù không tìm thấy cũng không báo lỗi ra ngoài
        if (userOpt.isEmpty()) {
            log.info("Yêu cầu quên mật khẩu cho email không tồn tại trong hệ thống: {}", target);
            return;
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("Tài khoản {} đang bị khóa nhưng yêu cầu quên mật khẩu", target);
            return;
        }

        Optional<VerificationChallenge> latestOpt = challengeRepository
                .findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(target, PURPOSE_PASSWORD_RESET);

        if (latestOpt.isPresent() && !latestOpt.get().canResend()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng đợi 60 giây trước khi yêu cầu mã OTP mới.");
        }

        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String secretDigest = sha256Hex(otpCode);

        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
        Instant resendAfter = Instant.now().plus(60, ChronoUnit.SECONDS);

        VerificationChallenge challenge = new VerificationChallenge(
                user.getId(),
                PURPOSE_PASSWORD_RESET,
                target,
                secretDigest,
                expiresAt,
                resendAfter
        );
        challengeRepository.save(challenge);

        emailService.sendOtpEmail(target, otpCode);
    }

    @Transactional
    public void resetPasswordWithOtp(ResetPasswordRequest req) {
        String target = req.email().trim().toLowerCase();
        VerificationChallenge challenge = challengeRepository
                .findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(target, PURPOSE_PASSWORD_RESET)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không tồn tại hoặc đã hết hạn."));

        validateAndConsumeChallenge(challenge, req.otpCode());

        User user = userRepository.findByEmailIgnoreCase(target)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.incrementSessionVersion();
        userRepository.save(user);

        log.info("Đặt lại mật khẩu thành công cho tài khoản: {}", target);
    }

    private void validateAndConsumeChallenge(VerificationChallenge challenge, String rawOtpCode) {
        if (challenge.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (challenge.getAttempts() >= 5) {
            challenge.setConsumedAt(Instant.now());
            challengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã bị vô hiệu do nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.");
        }

        String inputDigest = sha256Hex(rawOtpCode.trim());
        if (!challenge.getSecretDigest().equalsIgnoreCase(inputDigest)) {
            challenge.incrementAttempts();
            int remaining = 5 - challenge.getAttempts();
            if (remaining <= 0) {
                challenge.setConsumedAt(Instant.now());
                challengeRepository.save(challenge);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác. Bạn đã hết lượt thử.");
            }
            challengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        challenge.setVerifiedAt(Instant.now());
        challenge.setConsumedAt(Instant.now());
        challengeRepository.save(challenge);
    }

    private boolean isPhoneTaken(String phone, UUID excludeUserId) {
        String localForm = toLocalZeroPhone(phone);
        String plus84Form = toPlus84Phone(phone);
        if (excludeUserId == null) {
            return userRepository.existsByPhone(phone)
                    || userRepository.existsByPhone(localForm)
                    || userRepository.existsByPhone(plus84Form);
        }
        return userRepository.existsByPhoneAndIdNot(phone, excludeUserId)
                || userRepository.existsByPhoneAndIdNot(localForm, excludeUserId)
                || userRepository.existsByPhoneAndIdNot(plus84Form, excludeUserId);
    }

    private static String normalizePhoneOrNull(String raw) {
        if (raw == null) return null;
        String cleaned = raw.trim().replaceAll("[\\s.\\-()]", "");
        if (cleaned.isEmpty()) return null;
        if (cleaned.startsWith("+84")) {
            return "0" + cleaned.substring(3);
        }
        return cleaned;
    }

    private static String toPlus84Phone(String phone) {
        if (phone == null) return "";
        return phone.startsWith("0") ? "+84" + phone.substring(1) : phone;
    }

    private static String toLocalZeroPhone(String phone) {
        if (phone == null) return "";
        return phone.startsWith("+84") ? "0" + phone.substring(3) : phone;
    }

    private static boolean sameCanonicalPhone(String first, String second) {
        String a = normalizePhoneOrNull(first);
        String b = normalizePhoneOrNull(second);
        return Objects.equals(a, b);
    }

    private static String trimToNull(String value, int maxLen) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
