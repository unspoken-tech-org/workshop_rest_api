package com.tproject.workshop.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment details response")
public record PaymentResponseDto(
    @Schema(example = "1")
    Integer id,
    
    @Schema(hidden = true)
    LocalDateTime paymentDate,
    
    @Schema(example = "credito")
    String paymentType,
    
    @Schema(example = "150.00")
    BigDecimal paymentValue,
    
    @Schema(example = "repair")
    String category,
    
    @Schema(example = "10")
    Integer deviceId
) {
}
