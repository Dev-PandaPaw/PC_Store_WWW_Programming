package iuh.fit.nhom16.pcstore.identity.web;

import iuh.fit.nhom16.pcstore.identity.application.AuthService;
import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.model.AuthenticatedAccount;
import iuh.fit.nhom16.pcstore.identity.web.dto.ChangePasswordRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.UpdateProfileRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.UserDto;
import iuh.fit.nhom16.pcstore.identity.web.dto.VerifyPhoneOtpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        User user = authService.requireAuthenticatedUser(authentication);
        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UpdateProfileRequest req,
                                                 Authentication authentication) {
        User user = authService.requireAuthenticatedUser(authentication);
        UserDto updated = authService.updateProfile(user.getId(), req);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                                              Authentication authentication,
                                                              HttpServletRequest request) {
        User current = authService.requireAuthenticatedUser(authentication);
        User updated = authService.changePassword(current.getId(), req);

        // Giữ nguyên phiên đăng nhập hiện tại với sessionVersion mới, trong khi các phiên khác bị thu hồi
        var authorities = AuthController.toAuthorities(updated.getRoles());
        var newIdentity = new AuthenticatedAccount(updated.getId(), updated.getEmail(), updated.getSessionVersion());
        var newAuth = UsernamePasswordAuthenticationToken.authenticated(newIdentity, null, authorities);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(newAuth);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request != null ? request.getSession(false) : null;
        if (session != null) {
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        }

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được đăng xuất."));
    }

    @PostMapping("/phone/send-otp")
    public ResponseEntity<Map<String, String>> sendPhoneOtp(Authentication authentication) {
        User user = authService.requireAuthenticatedUser(authentication);
        authService.sendPhoneVerificationOtp(user.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Mã OTP 6 chữ số đã được gửi tới email tài khoản của bạn để xác minh số điện thoại."
        ));
    }

    @PostMapping("/phone/verify-otp")
    public ResponseEntity<UserDto> verifyPhoneOtp(@Valid @RequestBody VerifyPhoneOtpRequest req,
                                                  Authentication authentication) {
        User user = authService.requireAuthenticatedUser(authentication);
        UserDto updated = authService.verifyPhoneWithOtp(user.getId(), req.otpCode());
        return ResponseEntity.ok(updated);
    }
}

