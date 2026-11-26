package iuh.fit.nhom16.pcstore.identity.web;

import iuh.fit.nhom16.pcstore.identity.application.AdminUserService;
import iuh.fit.nhom16.pcstore.identity.web.dto.AdminUpdateUserStatusRequest;
import iuh.fit.nhom16.pcstore.identity.web.dto.AdminUserPageResponse;
import iuh.fit.nhom16.pcstore.identity.web.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<AdminUserPageResponse> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminUserService.searchUsers(query, status, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserStatusRequest req,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, req, authentication));
    }
}

