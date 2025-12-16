package com.tproject.workshop.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateDeviceOutputDtoRecord(
        @Schema(description = "Identifier of the newly created device", example = "101")
        int deviceId
) {

}
