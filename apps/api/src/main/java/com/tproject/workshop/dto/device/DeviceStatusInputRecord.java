package com.tproject.workshop.dto.device;

import com.tproject.workshop.enums.DeviceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record DeviceStatusInputRecord(
        @Schema(description = "New status of the device", example = "EM_ANDAMENTO")
                @NotNull(message = "O status do aparelho deve ser fornecido")
                DeviceStatusEnum deviceStatus) {}
