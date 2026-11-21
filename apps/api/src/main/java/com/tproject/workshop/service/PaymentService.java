package com.tproject.workshop.service;

import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.dto.payment.PaymentResponseDto;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.events.observability.ValueBand;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;
    private final DeviceRepository deviceRepository;
    private final ObservabilityEventRecorder eventRecorder;

    @Transactional
    public PaymentResponseDto create(PaymentDeviceInputDto inputPayment) {

        final var device =
                deviceRepository
                        .findById(inputPayment.deviceId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        String.format(
                                                "Aparelho com id %d não encontrado",
                                                inputPayment.deviceId())));

        final var paymentDate =
                Optional.ofNullable(inputPayment.paymentDate()).orElse(LocalDateTime.now());

        if (paymentDate.isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "A data do pagamento não pode ser maior que a data atual");
        }

        Payment paymentModel = new Payment();
        paymentModel.setPaymentValue(inputPayment.value());
        paymentModel.setPaymentType(inputPayment.paymentType());
        paymentModel.setCategory(inputPayment.category());
        paymentModel.setDevice(device);
        paymentModel.setPaymentDate(Timestamp.valueOf(paymentDate));
        paymentModel.setReceivedBy(inputPayment.receivedBy());

        Payment saved = repository.save(paymentModel);
        eventRecorder.record(
                ObservabilityEventCatalog.PAYMENT_CREATED,
                Map.of(
                        "payment.method", saved.getPaymentType().getDbValue(),
                        "payment.category", saved.getCategory().getDbValue(),
                        "payment.value_band", ValueBand.of(saved.getPaymentValue())));

        return toDto(saved);
    }

    private PaymentResponseDto toDto(Payment model) {
        return new PaymentResponseDto(
                model.getId(),
                model.getPaymentDate().toLocalDateTime(),
                model.getPaymentType(),
                model.getPaymentValue(),
                model.getCategory(),
                model.getDevice().getId(),
                model.getReceivedBy());
    }
}
