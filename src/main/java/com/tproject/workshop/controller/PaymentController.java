package com.tproject.workshop.controller;

import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.model.Payment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface PaymentController {

    @PostMapping
    Payment save(@RequestBody PaymentDeviceInputDto payment);
}
