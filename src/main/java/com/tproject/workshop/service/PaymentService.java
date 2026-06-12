package com.tproject.workshop.service;

import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.dto.payment.PaymentResponseDto;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.PaymentRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository repository;
    private final DeviceService deviceService;

    @Transactional
    public PaymentResponseDto save(PaymentDeviceInputDto payment) {
        String normalizedPaymentType = UtilsString.normalizeString(payment.paymentType()).toLowerCase();

        boolean hasInvalidPaymentType = Stream.of(
                normalizedPaymentType.equals("credito"),
                normalizedPaymentType.equals("debito"),
                normalizedPaymentType.equals("dinheiro"),
                normalizedPaymentType.equals("pix"),
                normalizedPaymentType.equals("outro")
        ).allMatch(value -> value.equals(false));

        if (hasInvalidPaymentType) {
            throw new BadRequestException(String.format("O tipo de pagamento \"%s\" não é válido. O tipo de pagamento deve ser um dos seguintes: credito, debito, dinheiro ou pix.", payment.paymentType()));
        }

        Payment paymentModel = new Payment();
        paymentModel.setPaymentValue(payment.value());
        paymentModel.setPaymentType(payment.paymentType());
        paymentModel.setCategory(payment.category());
        paymentModel.setDevice(new Device(payment.deviceId()));
        paymentModel.setPaymentDate(
                Optional.ofNullable(payment.paymentDate())
                        .map(Timestamp::valueOf)
                        .orElse(new Timestamp(System.currentTimeMillis()))
        );

        Payment saved = repository.save(paymentModel);

        return toDto(saved);
    }

    private PaymentResponseDto toDto(Payment model) {
        return new PaymentResponseDto(
                model.getId(),
                model.getPaymentDate().toLocalDateTime(),
                model.getPaymentType(),
                model.getPaymentValue(),
                model.getCategory(),
                model.getDevice().getId()
        );
    }
}
