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
    String recipientName,
    String province,
    String district,
    String ward,
    String addressLine,
    String status,
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
            user.getRecipientName(),
            user.getProvince(),
            user.getDistrict(),
            user.getWard(),
            user.getAddressLine(),
            user.getStatus(),
            user.getRoles(),
            user.getCreatedAt(), null
        );
    }
    public UserDto withCart(CartResponse cart) {
        return new UserDto(id, email, fullName, phone, recipientName, province, district, ward,
                addressLine, status, roles, createdAt, cart);
    }
}
