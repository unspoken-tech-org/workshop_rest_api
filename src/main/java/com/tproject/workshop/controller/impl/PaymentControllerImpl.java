package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.PaymentController;
import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/payment")
public class PaymentControllerImpl implements PaymentController {
    private final PaymentService service;

    @Override
    public Payment save(@Valid PaymentDeviceInputDto payment) {
        return service.save(payment);
    }
}
