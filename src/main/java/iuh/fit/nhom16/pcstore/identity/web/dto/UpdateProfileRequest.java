package iuh.fit.nhom16.pcstore.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 150, message = "Họ và tên tối đa 150 ký tự")
    String fullName,

    String phone,
    String recipientName,
    String province,
    String district,
    String ward,
    String addressLine
) {}
