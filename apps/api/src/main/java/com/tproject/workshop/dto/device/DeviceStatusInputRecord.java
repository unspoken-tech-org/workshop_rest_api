package com.tproject.workshop.dto.device;

import com.tproject.workshop.validation.ValidDeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record DeviceStatusInputRecord(
        @Schema(description = "New status of the device", example = "EM_ANDAMENTO")
        @NotEmpty(message = "O status do aparelho deve ser fornecido")
        @ValidDeviceStatus
        String deviceStatus
) {
}
