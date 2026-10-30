package iuh.fit.nhom16.pcstore.identity.application;

import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.domain.VerificationChallenge;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.infrastructure.VerificationChallengeRepository;
import iuh.fit.nhom16.pcstore.identity.web.dto.RegisterRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.ResetPasswordRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
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

        String phone = req.phone() != null && !req.phone().isBlank() ? req.phone().trim() : null;
        if (phone != null && userRepository.existsByPhone(phone)) {
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

        // Kiểm tra cooldown 60 giây
        Optional<VerificationChallenge> latestOpt = challengeRepository
                .findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(target, "PASSWORD_RESET");

        if (latestOpt.isPresent() && !latestOpt.get().canResend()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Vui lòng đợi 60 giây trước khi yêu cầu mã OTP mới.");
        }

        // Sinh mã OTP 6 chữ số
        String otpCode = String.format("%06d", secureRandom.nextInt(1_000_000));
        String secretDigest = sha256Hex(otpCode);

        Instant expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
        Instant resendAfter = Instant.now().plus(60, ChronoUnit.SECONDS);

        VerificationChallenge challenge = new VerificationChallenge(
                user.getId(),
                "PASSWORD_RESET",
                target,
                secretDigest,
                expiresAt,
                resendAfter
        );
        challengeRepository.save(challenge);

        // Gửi email thật
        emailService.sendOtpEmail(target, otpCode);
    }

    @Transactional
    public void resetPasswordWithOtp(ResetPasswordRequest req) {
        String target = req.email().trim().toLowerCase();
        VerificationChallenge challenge = challengeRepository
                .findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(target, "PASSWORD_RESET")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không tồn tại hoặc đã hết hạn."));

        if (challenge.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        if (challenge.getAttempts() >= 5) {
            challenge.setConsumedAt(Instant.now());
            challengeRepository.save(challenge);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã bị vô hiệu do nhập sai quá 5 lần. Vui lòng yêu cầu mã mới.");
        }

        String inputDigest = sha256Hex(req.otpCode().trim());
        if (!challenge.getSecretDigest().equalsIgnoreCase(inputDigest)) {
            challenge.incrementAttempts();
            challengeRepository.save(challenge);
            int remaining = 5 - challenge.getAttempts();
            if (remaining <= 0) {
                challenge.setConsumedAt(Instant.now());
                challengeRepository.save(challenge);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác. Bạn đã hết lượt thử.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        // Hợp lệ -> Tiến hành đổi mật khẩu
        User user = userRepository.findByEmailIgnoreCase(target)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.incrementSessionVersion();
        userRepository.save(user);

        challenge.setVerifiedAt(Instant.now());
        challenge.setConsumedAt(Instant.now());
        challengeRepository.save(challenge);

        log.info("Đặt lại mật khẩu thành công cho tài khoản: {}", target);
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
