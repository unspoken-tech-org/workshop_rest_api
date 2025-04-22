package com.tproject.workshop.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.Data;

@Data
public class PaymentDeviceOutputDto {

  private int paymentId;
  private LocalDate paymentDate;
  private String paymentType;
  private BigDecimal paymentValue;
  private String category;

  @JsonProperty("paymentDate")
  public String getEntryDate() {
    return paymentDate != null ? paymentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
  }
}
