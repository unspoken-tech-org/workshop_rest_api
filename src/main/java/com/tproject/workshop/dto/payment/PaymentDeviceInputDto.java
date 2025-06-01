package com.tproject.workshop.dto.payment;

import java.math.BigDecimal;


public record PaymentDeviceInputDto(Integer deviceId, String paymentType, BigDecimal value, String category) {}
