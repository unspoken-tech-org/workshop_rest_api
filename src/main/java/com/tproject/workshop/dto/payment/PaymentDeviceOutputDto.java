package com.tproject.workshop.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentDeviceOutputDto {

  private int paymentId;
  private LocalDateTime paymentDate;
  private String paymentType;
  private BigDecimal paymentValue;
  private String category;
}
