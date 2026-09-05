package com.tproject.workshop.dto.device;

import com.tproject.workshop.validation.ValidDeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DeviceUpdateInputDtoRecord(
        @Schema(description = "Identifier of the device being updated", example = "42")
        @NotNull(message = "O id do aparelho deve ser fornecido")
        Integer deviceId,
        @Schema(description = "New status of the device", example = "EM_ANDAMENTO")
        @NotEmpty(message = "O status do aparelho deve ser fornecido")
        @ValidDeviceStatus
        String deviceStatus,
        @Schema(description = "Updated problem description", example = "Replacing broken screen and battery")
        @NotEmpty(message = "O problema do aparelho não pode estar vazio. Adicione o problema")
        String problem,
        @Schema(description = "Optional observations about the service", example = "Awaiting customer confirmation for parts")
        String observation,
        @Schema(description = "Optional budget notes", example = "Initial budget approved")
        String budget,
        @Schema(description = "Budget fee charged at intake", example = "120.00")
        @DecimalMin(value = "0.00", message = "O valor do orçamento não pode ser negativo")
        BigDecimal budgetFee,
        @Schema(description = "Service cost estimate or charged amount", example = "220.00")
        @DecimalMin(value = "0.00", message = "O valor do serviço não pode ser negativo")
        BigDecimal serviceValue,
        @Schema(description = "Flags if the device is urgent", example = "true")
        boolean hasUrgency,
        @Schema(description = "Indicates if the device is under revision", example = "false")
        boolean revision,
        @Schema(description = "Technician responsible for the update", example = "7")
        Integer technicianId,
        @Schema(description = "Entry date (yyyy-MM-dd'T'HH:mm:ss)", example = "2026-06-15T00:00:00")
        LocalDateTime entryDate,
        @Schema(description = "Departure date (yyyy-MM-dd'T'HH:mm:ss)", example = "2026-06-20T00:00:00")
        LocalDateTime departureDate
) {

}
