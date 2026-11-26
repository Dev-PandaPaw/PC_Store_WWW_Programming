package iuh.fit.nhom16.pcstore.identity;

import iuh.fit.nhom16.pcstore.cart.service.CartStorageService;
import iuh.fit.nhom16.pcstore.identity.application.AdminUserService;
import iuh.fit.nhom16.pcstore.identity.application.AuthService;
import iuh.fit.nhom16.pcstore.identity.application.EmailService;
import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.domain.VerificationChallenge;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.infrastructure.VerificationChallengeRepository;
import iuh.fit.nhom16.pcstore.identity.model.AuthenticatedAccount;
import iuh.fit.nhom16.pcstore.identity.web.AccountController;
import iuh.fit.nhom16.pcstore.identity.web.AuthController;
import iuh.fit.nhom16.pcstore.identity.web.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IdentityAccountAndAdminTest {

    private UserRepository userRepository;
    private VerificationChallengeRepository challengeRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private CartStorageService cartStorageService;

    private AuthService authService;
    private AdminUserService adminUserService;
    private AuthController authController;
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        challengeRepository = mock(VerificationChallengeRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        emailService = mock(EmailService.class);
        cartStorageService = mock(CartStorageService.class);

        authService = new AuthService(userRepository, challengeRepository, passwordEncoder, emailService);
        adminUserService = new AdminUserService(userRepository);
        authController = new AuthController(authService, userRepository, cartStorageService);
        accountController = new AccountController(authService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Đổi mật khẩu xoay sessionVersion mới cho phiên hiện tại và vô hiệu hóa phiên cũ")
    void changePasswordRotatesCurrentSessionAndInvalidatesStaleSession() {
        UUID userId = UUID.randomUUID();
        User dbUser = new User("canh@pcstore.vn", passwordEncoder.encode("oldPass123"), "Võ Văn Cảnh", "0912345678");
        dbUser.setId(userId);
        dbUser.setSessionVersion(1L);

        when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Phiên B (trình duyệt khác) đang giữ sessionVersion = 1
        var staleAuth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(userId, dbUser.getEmail(), 1L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        // Phiên A thực hiện đổi mật khẩu
        MockHttpSession sessionA = new MockHttpSession();
        MockHttpServletRequest requestA = new MockHttpServletRequest();
        requestA.setSession(sessionA);

        var currentAuth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(userId, dbUser.getEmail(), 1L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        var response = accountController.changePassword(
                new ChangePasswordRequest("oldPass123", "newPass456"),
                currentAuth,
                requestA
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2L, dbUser.getSessionVersion());
        assertTrue(passwordEncoder.matches("newPass456", dbUser.getPasswordHash()));

        // Phiên A đã được cập nhật lên sessionVersion = 2 nên vẫn hợp lệ
        SecurityContext updatedCtx = (SecurityContext) sessionA.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(updatedCtx);
        User validCurrent = authService.requireAuthenticatedUser(updatedCtx.getAuthentication());
        assertEquals(userId, validCurrent.getId());

        // Phiên B vẫn giữ sessionVersion = 1 -> bị thu hồi với 401 Unauthorized và hủy session
        SecurityContext staleCtx = SecurityContextHolder.createEmptyContext();
        staleCtx.setAuthentication(staleAuth);
        SecurityContextHolder.setContext(staleCtx);
        MockHttpSession sessionB = new MockHttpSession();
        MockHttpServletRequest requestB = new MockHttpServletRequest();
        requestB.setSession(sessionB);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authController.getCurrentUser(requestB)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(sessionB.isInvalid());
    }

    @Test
    @DisplayName("Cập nhật hồ sơ lưu địa chỉ mặc định và reset xác minh nếu thay đổi số điện thoại")
    void updateProfileSavesAddressAndResetsPhoneVerificationWhenPhoneChanges() {
        UUID userId = UUID.randomUUID();
        User dbUser = new User("canh@pcstore.vn", "hash", "Võ Văn Cảnh", "0912345678");
        dbUser.setId(userId);
        dbUser.setPhoneVerifiedAt(Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Giữ nguyên SĐT cũ -> vẫn bảo toàn phoneVerified = true
        UserDto samePhoneResult = authService.updateProfile(userId, new UpdateProfileRequest(
                "Võ Văn Cảnh Updated",
                "+84912345678",
                "Cảnh Nhận Hàng",
                "Thành phố Hồ Chí Minh",
                "Quận Gò Vấp",
                "Phường 4",
                "12 Nguyễn Văn Bảo"
        ));
        assertTrue(samePhoneResult.phoneVerified());
        assertEquals("0912345678", samePhoneResult.phone());
        assertEquals("Thành phố Hồ Chí Minh", samePhoneResult.province());
        assertEquals("Quận Gò Vấp", samePhoneResult.district());

        // 2. Đổi sang SĐT mới -> phoneVerified chuyển về false để bắt buộc xác minh lại
        UserDto changedPhoneResult = authService.updateProfile(userId, new UpdateProfileRequest(
                "Võ Văn Cảnh Updated",
                "0987654321",
                "Cảnh Nhận Hàng",
                "Thành phố Hồ Chí Minh",
                "Quận Gò Vấp",
                "Phường 4",
                "12 Nguyễn Văn Bảo"
        ));
        assertFalse(changedPhoneResult.phoneVerified());
        assertEquals("0987654321", changedPhoneResult.phone());
    }

    @Test
    @DisplayName("Xác minh số điện thoại bằng mã OTP 6 chữ số hoạt động đúng quy trình")
    void verifyPhoneWithSixDigitOtpMarksPhoneVerified() {
        UUID userId = UUID.randomUUID();
        User dbUser = new User("canh@pcstore.vn", "hash", "Võ Văn Cảnh", "0912345678");
        dbUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(dbUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(challengeRepository.findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                eq("+84912345678"), eq("PHONE_VERIFY")
        )).thenReturn(Optional.empty());

        ArgumentCaptor<VerificationChallenge> challengeCaptor = ArgumentCaptor.forClass(VerificationChallenge.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);

        authService.sendPhoneVerificationOtp(userId);

        verify(challengeRepository).save(challengeCaptor.capture());
        verify(emailService).sendPhoneVerificationOtpEmail(eq("canh@pcstore.vn"), eq("0912345678"), otpCaptor.capture());

        VerificationChallenge savedChallenge = challengeCaptor.getValue();
        String sentOtp = otpCaptor.getValue();
        assertEquals(6, sentOtp.length());

        when(challengeRepository.findFirstByTargetIgnoreCaseAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                eq("+84912345678"), eq("PHONE_VERIFY")
        )).thenReturn(Optional.of(savedChallenge));

        UserDto verifiedDto = authService.verifyPhoneWithOtp(userId, sentOtp);
        assertTrue(verifiedDto.phoneVerified());
        assertNotNull(dbUser.getPhoneVerifiedAt());
        assertNotNull(savedChallenge.getConsumedAt());
    }

    @Test
    @DisplayName("Admin không thể tự khóa chính mình hoặc khóa Admin hoạt động cuối cùng, nhưng khóa được Customer")
    void adminUserRulesEnforceSelfLockAndLastAdminProtection() {
        UUID adminId = UUID.randomUUID();
        User admin = new User("admin@pcstore.vn", "hash", "Admin", "0901234567");
        admin.setId(adminId);
        admin.setRoles(Set.of("ROLE_ADMIN", "ROLE_CUSTOMER"));

        UUID otherAdminId = UUID.randomUUID();
        User otherAdmin = new User("admin2@pcstore.vn", "hash", "Admin 2", "0909999999");
        otherAdmin.setId(otherAdminId);
        otherAdmin.setRoles(Set.of("ROLE_ADMIN"));

        UUID customerId = UUID.randomUUID();
        User customer = new User("customer@pcstore.vn", "hash", "Customer", "0911222333");
        customer.setId(customerId);
        customer.setSessionVersion(1L);

        var actorAuth = UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedAccount(adminId, admin.getEmail(), 1L),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(otherAdminId)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.searchUsers(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(admin, customer)));

        // 1. Chặn tự khóa bản thân
        ResponseStatusException selfLockEx = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.updateUserStatus(adminId, new AdminUpdateUserStatusRequest("LOCKED", "Test"), actorAuth)
        );
        assertEquals(HttpStatus.BAD_REQUEST, selfLockEx.getStatusCode());

        // 2. Chặn khóa Admin cuối cùng khi chỉ còn 1 Admin active
        when(userRepository.countActiveAdmins()).thenReturn(1L);
        ResponseStatusException lastAdminEx = assertThrows(
                ResponseStatusException.class,
                () -> adminUserService.updateUserStatus(otherAdminId, new AdminUpdateUserStatusRequest("LOCKED", "Nghỉ việc"), actorAuth)
        );
        assertEquals(HttpStatus.CONFLICT, lastAdminEx.getStatusCode());

        // 3. Khóa Customer hợp lệ -> chuyển LOCKED, lưu lý do và tăng sessionVersion
        UserDto lockedCustomer = adminUserService.updateUserStatus(
                customerId,
                new AdminUpdateUserStatusRequest("LOCKED", "Vi phạm chính sách đặt hàng ảo"),
                actorAuth
        );
        assertEquals("LOCKED", lockedCustomer.status());
        assertEquals("Vi phạm chính sách đặt hàng ảo", lockedCustomer.statusReason());
        assertEquals(2L, customer.getSessionVersion());

        // 4. Mở khóa lại Customer -> ACTIVE, xóa lý do
        UserDto unlockedCustomer = adminUserService.updateUserStatus(
                customerId,
                new AdminUpdateUserStatusRequest("ACTIVE", null),
                actorAuth
        );
        assertEquals("ACTIVE", unlockedCustomer.status());
        assertNull(unlockedCustomer.statusReason());

        // 5. Danh sách trả về đúng phân trang
        AdminUserPageResponse page = adminUserService.searchUsers(null, "ALL", 0, 20);
        assertEquals(2, page.items().size());
    }
}

