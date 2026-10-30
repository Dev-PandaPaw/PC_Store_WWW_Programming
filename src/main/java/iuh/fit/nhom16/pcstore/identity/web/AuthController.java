package iuh.fit.nhom16.pcstore.identity.web;

import iuh.fit.nhom16.pcstore.identity.application.AuthService;
import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.web.dto.ForgotPasswordRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.LoginRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.RegisterRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.ResetPasswordRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest req) {
        UserDto created = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        User user = authService.authenticate(req.username(), req.password());

        // Tạo Spring Security Authentication
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);

        // Lưu Authentication vào SecurityContext và HttpSession
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }

        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không tồn tại"));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa");
        }

        return ResponseEntity.ok(UserDto.fromEntity(user));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.initiateForgotPassword(req.email());
        return ResponseEntity.ok(Map.of(
                "message", "Nếu email tồn tại trong hệ thống, mã xác thực OTP đã được gửi đến hòm thư của bạn."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPasswordWithOtp(req);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập với mật khẩu mới."));
    }
}
