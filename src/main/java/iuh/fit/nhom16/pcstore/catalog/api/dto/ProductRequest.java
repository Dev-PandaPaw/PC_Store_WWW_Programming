package iuh.fit.nhom16.pcstore.catalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 100) String sku,
        @NotBlank @Size(max = 255) String slug,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String brand,
        String description,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @PositiveOrZero Integer stockOnHand,
        @PositiveOrZero Short warrantyMonths,
        @Valid ProductSpecRequest spec
) {
}
