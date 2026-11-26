package com.tproject.workshop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.dto.payment.PaymentResponseDto;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.enums.PaymentMethodEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.integration.support.TestClock;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
import com.tproject.workshop.support.fixtures.DeviceTestBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final int DEVICE_ID = 10;
    private static final LocalDate TODAY = LocalDate.now(TestClock.FIXED);

    @Mock private PaymentRepository paymentRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ObservabilityEventRecorder eventRecorder;
    @Spy private Clock clock = TestClock.FIXED;

    @InjectMocks private PaymentService paymentService;

    private Device device;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        device =
                DeviceTestBuilder.aDevice()
                        .withId(DEVICE_ID)
                        .withStatus(DeviceStatusEnum.EM_ANDAMENTO)
                        .withEntryDate(Timestamp.valueOf(TODAY.minusDays(2).atTime(14, 30)))
                        .withBudgetFee(new BigDecimal("50.00"))
                        .withServiceValue(new BigDecimal("200.00"))
                        .withBudgetFeeCollected(false)
                        .build();
    }

    private void stubDeviceFound() {
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
    }

    private void stubPaymentSave() {
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(1);
                    return p;
                });
    }

    @Test
    void create_shouldPersistServicePayment_whenWithinServiceValue() {
        stubDeviceFound();
        stubPaymentSave();
        when(paymentRepository.sumPaymentValueByDeviceId(DEVICE_ID))
                .thenReturn(new BigDecimal("50.00"));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("150.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.paymentValue()).isEqualByComparingTo("150.00");
        assertThat(result.category()).isEqualTo(PaymentCategoryEnum.SERVICOS);
        verify(eventRecorder).record(eq(ObservabilityEventCatalog.PAYMENT_CREATED), anyMap());
    }

    @Test
    void create_shouldAllowSameDayMidnightPayment_whenEntryDateIsLaterInSameDay() {
        stubDeviceFound();
        stubPaymentSave();
        when(paymentRepository.sumPaymentValueByDeviceId(DEVICE_ID)).thenReturn(BigDecimal.ZERO);
        device.setEntryDate(Timestamp.valueOf(TODAY.atTime(15, 45)));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.CREDITO,
                        new BigDecimal("100.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.paymentValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_shouldSetBudgetFeeCollected_whenBudgetFeePaymentReachesBudgetFee() {
        stubDeviceFound();
        stubPaymentSave();
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        DEVICE_ID, PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                .thenReturn(BigDecimal.ZERO);
        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.DINHEIRO,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        TODAY.atStartOfDay(),
                        "Maria");

        paymentService.create(dto);

        assertThat(device.isBudgetFeeCollected()).isTrue();
        verify(deviceRepository).save(device);
    }

    @Test
    void create_shouldAllowBudgetFeePayment_whenServiceValueIsNull() {
        stubDeviceFound();
        stubPaymentSave();
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        DEVICE_ID, PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                .thenReturn(BigDecimal.ZERO);
        device.setServiceValue(null);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        TODAY.atStartOfDay(),
                        "Maria");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.category()).isEqualTo(PaymentCategoryEnum.TAXA_ORCAMENTO);
        assertThat(device.isBudgetFeeCollected()).isTrue();
    }

    @Test
    void create_shouldAllowBudgetFeePayment_whenDeviceIsNovo() {
        stubDeviceFound();
        stubPaymentSave();
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        DEVICE_ID, PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                .thenReturn(BigDecimal.ZERO);
        device.setDeviceStatus(DeviceStatusEnum.NOVO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        TODAY.atStartOfDay(),
                        "Maria");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.category()).isEqualTo(PaymentCategoryEnum.TAXA_ORCAMENTO);
        assertThat(device.isBudgetFeeCollected()).isTrue();
    }

    @Test
    void create_shouldThrowBadRequest_whenServicosPaymentOnNovoDevice() {
        stubDeviceFound();
        device.setDeviceStatus(DeviceStatusEnum.NOVO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = PaymentCategoryEnum.class,
            names = {"TAXA_ORCAMENTO", "SERVICOS"})
    void create_shouldThrowBadRequest_whenDeviceIsDescartado(PaymentCategoryEnum category) {
        stubDeviceFound();
        device.setDeviceStatus(DeviceStatusEnum.DESCARTADO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        category,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowBadRequest_whenPaymentDateBeforeEntryDate() {
        stubDeviceFound();
        LocalDateTime beforeEntry =
                device.getEntryDate().toLocalDateTime().toLocalDate().minusDays(1).atStartOfDay();

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.SERVICOS,
                        beforeEntry,
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowBadRequest_whenTotalPaidExceedsServiceValue() {
        stubDeviceFound();
        when(paymentRepository.sumPaymentValueByDeviceId(DEVICE_ID))
                .thenReturn(new BigDecimal("150.00"));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.CREDITO,
                        new BigDecimal("60.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowBadRequest_whenServiceValueNotDefinedForServicos() {
        stubDeviceFound();
        device.setServiceValue(null);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.CREDITO,
                        new BigDecimal("60.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_shouldThrowBadRequest_whenBudgetFeeNotDefinedForTaxaOrcamento() {
        stubDeviceFound();
        device.setBudgetFee(BigDecimal.ZERO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("20.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_shouldThrowBadRequest_whenBudgetFeePaymentsExceedBudgetFee() {
        stubDeviceFound();
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        DEVICE_ID, PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                .thenReturn(new BigDecimal("30.00"));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("25.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_shouldThrowNotFound_whenDeviceDoesNotExist() {
        when(deviceRepository.findById(999)).thenReturn(Optional.empty());

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        999,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void dtoValidation_shouldRejectMoreThanTwoDecimalPlacesOrThirteenIntegerDigits() {
        PaymentDeviceInputDto tooManyDecimals =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("10.005"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        PaymentDeviceInputDto tooManyIntegers =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("12345678901234.00"),
                        PaymentCategoryEnum.SERVICOS,
                        TODAY.atStartOfDay(),
                        "João");

        assertThat(validator.validate(tooManyDecimals)).isNotEmpty();
        assertThat(validator.validate(tooManyIntegers)).isNotEmpty();
    }

    @Test
    void deviceUpdateDtoValidation_shouldRejectNullBudgetFeeAndInvalidDigits() {
        DeviceUpdateInputDtoRecord nullBudgetFee =
                new DeviceUpdateInputDtoRecord(
                        1,
                        "EM_ANDAMENTO",
                        "Problema",
                        null,
                        null,
                        null,
                        new BigDecimal("100.00"),
                        false,
                        false,
                        null,
                        null,
                        null);

        DeviceUpdateInputDtoRecord invalidServiceScale =
                new DeviceUpdateInputDtoRecord(
                        1,
                        "EM_ANDAMENTO",
                        "Problema",
                        null,
                        null,
                        new BigDecimal("50.00"),
                        new BigDecimal("100.005"),
                        false,
                        false,
                        null,
                        null,
                        null);

        assertThat(validator.validate(nullBudgetFee)).isNotEmpty();
        assertThat(validator.validate(invalidServiceScale)).isNotEmpty();
    }
}
