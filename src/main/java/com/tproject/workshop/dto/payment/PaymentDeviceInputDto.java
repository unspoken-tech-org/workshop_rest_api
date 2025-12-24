package com.tproject.workshop.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentDeviceInputDto(
        @Schema(description = "Device identifier receiving the payment", example = "12")
        @NotNull(message = "ID do dispositivo não pode ser nulo")
        Integer deviceId,

        @Schema(description = "Payment method used by the customer", example = "credit")
        @NotBlank(message = "Tipo de pagamento não pode estar vazio")
        String paymentType,

        @Schema(description = "Amount collected for this transaction", example = "150.00")
        @NotNull(message = "Valor do pagamento não pode ser nulo")
        @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Category or purpose of the payment", example = "partial")
        String category
) {
}
