package iuh.fit.nhom16.pcstore.identity.web.dto;

import iuh.fit.nhom16.pcstore.identity.domain.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import iuh.fit.nhom16.pcstore.cart.dto.CartDtos.CartResponse;

public record UserDto(
    UUID id,
    String email,
    String fullName,
    String phone,
    boolean phoneVerified,
    String recipientName,
    String province,
    String district,
    String ward,
    String addressLine,
    String status,
    String statusReason,
    Set<String> roles,
    Instant createdAt,
    CartResponse cart
) {
    public static UserDto fromEntity(User user) {
        if (user == null) return null;
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getPhoneVerifiedAt() != null,
            user.getRecipientName(),
            user.getProvince(),
            user.getDistrict(),
            user.getWard(),
            user.getAddressLine(),
            user.getStatus(),
            user.getStatusReason(),
            user.getRoles(),
            user.getCreatedAt(), null
        );
    }
    public UserDto withCart(CartResponse cart) {
        return new UserDto(id, email, fullName, phone, phoneVerified, recipientName, province, district, ward,
                addressLine, status, statusReason, roles, createdAt, cart);
    }
}
