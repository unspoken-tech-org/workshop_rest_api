package com.tproject.workshop.service;

import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;

    public Payment save(PaymentDeviceInputDto payment) {
        Payment paymentModel = new Payment();
        paymentModel.setPaymentValue(payment.value());
        paymentModel.setPaymentType(payment.paymentType());
        paymentModel.setCategory(payment.category());
        paymentModel.setDevice(new Device(payment.deviceId()));

        return repository.save(paymentModel);
    }
}
