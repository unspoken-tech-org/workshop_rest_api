package com.tproject.workshop.service;

import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.dto.payment.PaymentResponseDto;
import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.events.observability.ValueBand;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository repository;
    private final DeviceRepository deviceRepository;
    private final ObservabilityEventRecorder eventRecorder;
    private final Clock clock;

    @Transactional
    public PaymentResponseDto create(PaymentDeviceInputDto inputPayment) {

        final var device =
                deviceRepository
                        .findById(inputPayment.deviceId())
                        .orElseThrow(
                                () -> NotFoundException.of("Aparelho", inputPayment.deviceId()));

        if (!device.getDeviceStatus().allowsPayment(inputPayment.category())) {
            throw new BadRequestException(
                    "Não é possível registrar pagamento da categoria %s para aparelho com status %s",
                    inputPayment.category().getDbValue(), device.getDeviceStatus().name());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        final var paymentDate = Optional.ofNullable(inputPayment.paymentDate()).orElse(now);

        if (paymentDate.isAfter(now)) {
            throw new BadRequestException(
                    "A data do pagamento não pode ser maior que a data atual");
        }

        if (device.getEntryDate() != null
                && paymentDate
                        .toLocalDate()
                        .isBefore(device.getEntryDate().toLocalDateTime().toLocalDate())) {
            throw new BadRequestException(
                    "A data do pagamento não pode ser anterior à data de entrada do aparelho");
        }

        validateAndReconcileTotals(device, inputPayment.category(), inputPayment.value());

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

    private void validateAndReconcileTotals(
            Device device, PaymentCategoryEnum category, BigDecimal amount) {
        BigDecimal totalPaid =
                Optional.ofNullable(repository.sumPaymentValueByDeviceId(device.getId()))
                        .orElse(BigDecimal.ZERO);

        if (category == PaymentCategoryEnum.TAXA_ORCAMENTO) {
            BigDecimal budgetFee =
                    Optional.ofNullable(device.getBudgetFee()).orElse(BigDecimal.ZERO);
            if (budgetFee.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Taxa de orçamento não definida para este aparelho");
            }

            BigDecimal totalPaidFee =
                    Optional.ofNullable(
                                    repository.sumPaymentValueByDeviceIdAndCategory(
                                            device.getId(),
                                            PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                            .orElse(BigDecimal.ZERO);
            BigDecimal newTotalPaidFee = totalPaidFee.add(amount);

            if (newTotalPaidFee.compareTo(budgetFee) > 0) {
                throw new BadRequestException(
                        "O valor total pago em taxas de orçamento excede a taxa de orçamento do aparelho");
            }

            if (device.getServiceValue() != null
                    && device.getServiceValue().compareTo(BigDecimal.ZERO) > 0
                    && totalPaid.add(amount).compareTo(device.getServiceValue()) > 0) {
                throw new BadRequestException("O valor total pago excede o valor do serviço");
            }

            if (newTotalPaidFee.compareTo(budgetFee) >= 0 && !device.isBudgetFeeCollected()) {
                device.setBudgetFeeCollected(true);
                deviceRepository.save(device);
            }
            return;
        }

        BigDecimal serviceValue = device.getServiceValue();
        if (serviceValue == null || serviceValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Valor do serviço não definido para este aparelho");
        }
        if (totalPaid.add(amount).compareTo(serviceValue) > 0) {
            throw new BadRequestException("O valor total pago excede o valor do serviço");
        }
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
