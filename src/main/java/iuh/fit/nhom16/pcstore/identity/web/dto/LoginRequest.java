package iuh.fit.nhom16.pcstore.identity.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email hoặc tên đăng nhập không được để trống")
    String username,

    @NotBlank(message = "Mật khẩu không được để trống")
    String password
) {}
