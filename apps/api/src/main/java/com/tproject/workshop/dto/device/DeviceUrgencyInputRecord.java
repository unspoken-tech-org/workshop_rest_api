package com.tproject.workshop.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeviceUrgencyInputRecord(
        @Schema(description = "Flags if the device is urgent", example = "true")
        boolean hasUrgency
) {
}
