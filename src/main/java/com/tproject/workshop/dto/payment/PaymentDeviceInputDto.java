package com.tproject.workshop.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentDeviceInputDto(
    @NotNull(message = "ID do dispositivo não pode ser nulo")
    Integer deviceId,
    
    @NotBlank(message = "Tipo de pagamento não pode estar vazio")
    String paymentType,
    
    @NotNull(message = "Valor do pagamento não pode ser nulo")
    @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser maior que zero")
    BigDecimal value,
    
    String category
) {}
