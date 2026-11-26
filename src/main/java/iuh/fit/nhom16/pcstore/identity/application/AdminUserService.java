package iuh.fit.nhom16.pcstore.identity.application;

import iuh.fit.nhom16.pcstore.identity.domain.User;
import iuh.fit.nhom16.pcstore.identity.infrastructure.UserRepository;
import iuh.fit.nhom16.pcstore.identity.model.AuthenticatedAccount;
import iuh.fit.nhom16.pcstore.identity.web.dto.AdminUpdateUserStatusRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.AdminUserPageResponse;
import iuh.fit.nhom16.pcstore.identity.web.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse searchUsers(String query, String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedQuery = query != null && !query.isBlank() ? query.trim() : null;
        String normalizedStatus = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())
                ? status.trim().toUpperCase(Locale.ROOT)
                : null;

        Page<User> result = userRepository.searchUsers(
                normalizedQuery,
                normalizedStatus,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<UserDto> items = result.getContent().stream()
                .map(UserDto::fromEntity)
                .toList();

        return new AdminUserPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản người dùng."));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateUserStatus(UUID targetUserId, AdminUpdateUserStatusRequest req, Authentication actorAuth) {
        String desiredStatus = req.status() != null ? req.status().trim().toUpperCase(Locale.ROOT) : "";
        if (!"ACTIVE".equals(desiredStatus) && !"LOCKED".equals(desiredStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ. Chỉ chấp nhận ACTIVE hoặc LOCKED.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản người dùng."));

        UUID actorId = resolveActorId(actorAuth);
        if ("LOCKED".equals(desiredStatus)) {
            if (actorId != null && actorId.equals(target.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không thể tự khóa tài khoản quản trị đang đăng nhập.");
            }

            if (isAdminUser(target) && target.isActive()) {
                long activeAdmins = userRepository.countActiveAdmins();
                if (activeAdmins <= 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể khóa quản trị viên hoạt động cuối cùng của hệ thống.");
                }
            }

            String reason = req.reason() != null ? req.reason().trim() : "";
            if (reason.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do khóa tài khoản.");
            }

            target.setStatus("LOCKED");
            target.setStatusReason(reason);
            target.incrementSessionVersion();
        } else {
            target.setStatus("ACTIVE");
            target.setStatusReason(null);
        }

        return UserDto.fromEntity(userRepository.save(target));
    }

    private UUID resolveActorId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof AuthenticatedAccount account) {
            return account.userId();
        }
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .map(User::getId)
                .orElse(null);
    }

    private boolean isAdminUser(User user) {
        if (user.getRoles() == null) return false;
        return user.getRoles().stream()
                .anyMatch(role -> "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role));
    }
}

