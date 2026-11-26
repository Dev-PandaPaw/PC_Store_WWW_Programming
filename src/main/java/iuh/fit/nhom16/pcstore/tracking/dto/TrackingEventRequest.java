package iuh.fit.nhom16.pcstore.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record TrackingEventRequest(
        @NotBlank(message = "eventType không được để trống")
        @Size(max = 50, message = "eventType tối đa 50 ký tự")
        String eventType,

        UUID productId,

        @Size(max = 100, message = "categorySlug tối đa 100 ký tự")
        String categorySlug,

        String metadata,

        String clientTimestamp
) {
}

