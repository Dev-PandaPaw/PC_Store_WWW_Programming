package iuh.fit.nhom16.pcstore.catalog.api.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductSpecRequest(
        @Size(max = 255) String cpu,
        @Size(max = 255) String gpu,
        @PositiveOrZero Integer ramGb,
        @PositiveOrZero Integer storageGb,
        @Size(max = 100) String storageType,
        @Size(max = 255) String motherboard,
        @Size(max = 255) String psu,
        @Size(max = 255) String caseName,
        @Size(max = 255) String operatingSystem
) {
}
