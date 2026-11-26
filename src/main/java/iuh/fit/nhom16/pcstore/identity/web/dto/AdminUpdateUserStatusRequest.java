package iuh.fit.nhom16.pcstore.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserStatusRequest(
    @NotBlank(message = "Trạng thái không được để trống")
    String status,

    @Size(max = 255, message = "Lý do tối đa 255 ký tự")
    String reason
) {}

