package com.tproject.workshop.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    @JsonProperty("paymentDate")
    @Schema(name = "paymentDate", example = "16/01/2024")
    public String getFormattedPaymentDate() {
        return paymentDate != null ? paymentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }
}
