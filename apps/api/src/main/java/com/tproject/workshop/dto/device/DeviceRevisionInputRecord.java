package com.tproject.workshop.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeviceRevisionInputRecord(
        @Schema(description = "Indicates if the device is under revision", example = "false")
        boolean revision
) {
}
