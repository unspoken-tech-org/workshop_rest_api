package com.tproject.workshop.dto.payment;

import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.enums.PaymentMethodEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentDeviceOutputDto {

  private int paymentId;
  private LocalDateTime paymentDate;
  private PaymentMethodEnum paymentType;
  private BigDecimal paymentValue;
  private PaymentCategoryEnum category;
  private String receivedBy;
}
