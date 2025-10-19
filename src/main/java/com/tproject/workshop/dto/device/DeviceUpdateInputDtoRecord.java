package com.tproject.workshop.dto.device;

import com.tproject.workshop.validation.ValidDeviceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeviceUpdateInputDtoRecord(
        @NotNull(message = "O id do aparelho deve ser fornecido")
        Integer deviceId,
        @NotEmpty(message = "O status do aparelho deve ser fornecido")
        @ValidDeviceStatus
        String deviceStatus,
        @NotEmpty(message = "O problema do aparelho não pode ser vazio. Adicione o problema")
        String problem,
        String observation,
        String budget,
        @DecimalMin(value = "0.00", message = "O valor do orçamento não pode ser negativo")
        BigDecimal laborValue,
        @DecimalMin(value = "0.00", message = "O valor do serviço não pode ser negativo")
        BigDecimal serviceValue,
        boolean laborValueCollected,
        boolean hasUrgency,
        boolean revision,
        Integer technicianId
) {

}
