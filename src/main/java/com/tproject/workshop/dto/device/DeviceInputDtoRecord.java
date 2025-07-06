package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record DeviceInputDtoRecord(
        @NotNull(message = "O id do cliente é obrigatório")
        Integer customerId,
        @NotNull(message = "A relação entre tipo, marca e modelo do aparelho é obrigatória")
        TypeBrandModelInputDtoRecord typeBrandModel,
        Integer technicianId,
        @NotBlank(message = "O problema do aparelho é obrigatório")
        String problem,
        String observation,
        BigDecimal budgetValue,
        @NotEmpty(message = "A lista de cores é obrigatória")
        List<ColorInputDtoRecord> colors,
        boolean hasUrgency

) {
}

