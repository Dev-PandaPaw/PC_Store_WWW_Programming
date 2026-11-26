package iuh.fit.nhom16.pcstore.identity.web.dto;

import java.util.List;

public record AdminUserPageResponse(
    List<UserDto> items,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}

