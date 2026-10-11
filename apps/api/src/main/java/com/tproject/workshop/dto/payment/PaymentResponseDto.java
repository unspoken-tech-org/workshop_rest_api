package com.tproject.workshop.dto.payment;

import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.enums.PaymentMethodEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment details response")
public record PaymentResponseDto(
    @Schema(example = "1")
    Integer id,
    
    @Schema(description = "Date and time of the payment", example = "2026-06-01T00:00:00")
    LocalDateTime paymentDate,
    
    @Schema(example = "credito")
    PaymentMethodEnum paymentType,
    
    @Schema(example = "150.00")
    BigDecimal paymentValue,
    
    @Schema(example = "servicos")
    PaymentCategoryEnum category,
    
    @Schema(example = "10")
    Integer deviceId,

    @Schema(description = "Name of the person who received the payment", example = "João da Silva")
    String receivedBy
) {
}
