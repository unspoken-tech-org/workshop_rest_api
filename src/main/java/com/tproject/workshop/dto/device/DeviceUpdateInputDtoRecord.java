package com.tproject.workshop.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tproject.workshop.validation.ValidDeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        @Schema(description = "Labor cost estimate or charged amount", example = "120.00")
        @DecimalMin(value = "0.00", message = "O valor do orçamento não pode ser negativo")
        BigDecimal laborValue,
        @Schema(description = "Service cost estimate or charged amount", example = "220.00")
        @DecimalMin(value = "0.00", message = "O valor do serviço não pode ser negativo")
        BigDecimal serviceValue,
        @Schema(description = "Discount applied to the device", example = "30.00")
        @DecimalMin(value = "0.00", message = "O desconto não pode ser negativo")
        BigDecimal discount,
        @Schema(description = "Indicates if labor value was collected", example = "false")
        boolean laborValueCollected,
        @Schema(description = "Flags if the device is urgent", example = "true")
        boolean hasUrgency,
        @Schema(description = "Indicates if the device is under revision", example = "false")
        boolean revision,
        @Schema(description = "Technician responsible for the update", example = "7")
        Integer technicianId,
        @Schema(description = "Entry date (dd/MM/yyyy)", example = "15/06/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate entryDate,
        @Schema(description = "Departure date (dd/MM/yyyy)", example = "20/06/2026")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate departureDate
) {

}
