package iuh.fit.nhom16.pcstore.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductStatusRequest(@NotBlank String status) {
}
