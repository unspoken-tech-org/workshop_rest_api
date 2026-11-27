package com.tproject.workshop.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record DeviceInputDtoRecord(
        @Schema(description = "Owner customer identifier", example = "15")
                @NotNull(message = "O id do cliente é obrigatório")
                Integer customerId,
        @Schema(description = "Type/brand/model mapping for catalog consistency")
                @NotNull(message = "A relação entre tipo, marca e modelo do aparelho é obrigatória")
                @Valid
                TypeBrandModelInputDtoRecord typeBrandModel,
        @Schema(description = "Technician assigned at intake, optional", example = "8")
                Integer technicianId,
        @Schema(description = "Reported device issue", example = "Leaking")
                @NotBlank(message = "O problema do aparelho é obrigatório")
                String problem,
        @Schema(
                        description = "Additional notes about the device",
                        example = "Customer requests OEM parts only")
                String observation,
        @Schema(description = "Budget fee charged at intake", example = "350.00")
                @DecimalMin(value = "0.00", message = "O valor do orçamento não pode ser negativo")
                @Digits(
                        integer = 13,
                        fraction = 2,
                        message =
                                "O valor do orçamento deve ter no máximo 13 dígitos inteiros e 2 decimais")
                BigDecimal budgetFee,
        @Schema(description = "Selected colors for the device", example = "[\"black\",\"blue\"]")
                @NotEmpty(message = "A lista de cores é obrigatória")
                List<@NotBlank(message = "A cor não pode ser vazia") String> colors,
        @Schema(description = "Marks if the device requires priority handling", example = "false")
                boolean hasUrgency,
        @Schema(
                        description =
                                "Order this visit comes from (revision or return); reuses its physical device",
                        example = "1423")
                Integer originOrderId,
        @Schema(description = "Opens the order as a warranty revision", example = "false")
                boolean revision) {}
