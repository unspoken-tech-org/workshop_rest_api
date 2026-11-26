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
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.Payment;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    private static final int DEVICE_ID = 10;

    @Mock private PaymentRepository paymentRepository;
    @Mock private DeviceRepository deviceRepository;
    @Mock private ObservabilityEventRecorder eventRecorder;
    @Spy private Clock clock = Clock.systemDefaultZone();

    @InjectMocks private PaymentService paymentService;

    private Device device;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();

        device = new Device();
        device.setId(DEVICE_ID);
        device.setDeviceStatus(DeviceStatusEnum.EM_ANDAMENTO);
        device.setEntryDate(Timestamp.valueOf(LocalDate.now().minusDays(2).atTime(14, 30)));
        device.setBudgetFee(new BigDecimal("50.00"));
        device.setServiceValue(new BigDecimal("200.00"));
        device.setBudgetFeeCollected(false);

        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
        when(paymentRepository.sumPaymentValueByDeviceId(DEVICE_ID)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        DEVICE_ID, PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment p = invocation.getArgument(0);
                    p.setId(1);
                    return p;
                });
    }

    @Test
    void create_shouldPersistServicePayment_whenWithinServiceValue() {
        when(paymentRepository.sumPaymentValueByDeviceId(DEVICE_ID))
                .thenReturn(new BigDecimal("50.00"));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("150.00"),
                        PaymentCategoryEnum.SERVICOS,
                        LocalDate.now().atStartOfDay(),
                        "João");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.paymentValue()).isEqualByComparingTo("150.00");
        assertThat(result.category()).isEqualTo(PaymentCategoryEnum.SERVICOS);
        verify(eventRecorder).record(eq(ObservabilityEventCatalog.PAYMENT_CREATED), anyMap());
    }

    @Test
    void create_shouldAllowSameDayMidnightPayment_whenEntryDateIsLaterInSameDay() {
        device.setEntryDate(Timestamp.valueOf(LocalDate.now().atTime(15, 45)));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.CREDITO,
                        new BigDecimal("100.00"),
                        PaymentCategoryEnum.SERVICOS,
                        LocalDate.now().atStartOfDay(),
                        "João");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.paymentValue()).isEqualByComparingTo("100.00");
    }

    @Test
    void create_shouldSetBudgetFeeCollected_whenBudgetFeePaymentReachesBudgetFee() {
        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.DINHEIRO,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        LocalDate.now().atStartOfDay(),
                        "Maria");

        paymentService.create(dto);

        assertThat(device.isBudgetFeeCollected()).isTrue();
        verify(deviceRepository).save(device);
    }

    @Test
    void create_shouldAllowBudgetFeePayment_whenServiceValueIsNull() {
        device.setServiceValue(null);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        LocalDate.now().atStartOfDay(),
                        "Maria");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.category()).isEqualTo(PaymentCategoryEnum.TAXA_ORCAMENTO);
        assertThat(device.isBudgetFeeCollected()).isTrue();
    }

    @Test
    void create_shouldAllowBudgetFeePayment_whenDeviceIsNovo() {
        device.setDeviceStatus(DeviceStatusEnum.NOVO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        LocalDate.now().atStartOfDay(),
                        "Maria");

        PaymentResponseDto result = paymentService.create(dto);

        assertThat(result.category()).isEqualTo(PaymentCategoryEnum.TAXA_ORCAMENTO);
        assertThat(device.isBudgetFeeCollected()).isTrue();
    }

    @Test
    void create_shouldThrowBadRequest_whenServicosPaymentOnNovoDevice() {
        device.setDeviceStatus(DeviceStatusEnum.NOVO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        PaymentCategoryEnum.SERVICOS,
                        LocalDate.now().atStartOfDay(),
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
        device.setDeviceStatus(DeviceStatusEnum.DESCARTADO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("50.00"),
                        category,
                        LocalDate.now().atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowBadRequest_whenPaymentDateBeforeEntryDate() {
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
        when(paymentRepository.sumPaymentValueByDeviceId(DEVICE_ID))
                .thenReturn(new BigDecimal("150.00"));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.CREDITO,
                        new BigDecimal("60.00"),
                        PaymentCategoryEnum.SERVICOS,
                        LocalDate.now().atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowBadRequest_whenServiceValueNotDefinedForServicos() {
        device.setServiceValue(null);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.CREDITO,
                        new BigDecimal("60.00"),
                        PaymentCategoryEnum.SERVICOS,
                        LocalDate.now().atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_shouldThrowBadRequest_whenBudgetFeeNotDefinedForTaxaOrcamento() {
        device.setBudgetFee(BigDecimal.ZERO);

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("20.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        LocalDate.now().atStartOfDay(),
                        "João");

        assertThatThrownBy(() -> paymentService.create(dto))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_shouldThrowBadRequest_whenBudgetFeePaymentsExceedBudgetFee() {
        when(paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        DEVICE_ID, PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()))
                .thenReturn(new BigDecimal("30.00"));

        PaymentDeviceInputDto dto =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("25.00"),
                        PaymentCategoryEnum.TAXA_ORCAMENTO,
                        LocalDate.now().atStartOfDay(),
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
                        LocalDate.now().atStartOfDay(),
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
                        LocalDate.now().atStartOfDay(),
                        "João");

        PaymentDeviceInputDto tooManyIntegers =
                new PaymentDeviceInputDto(
                        DEVICE_ID,
                        PaymentMethodEnum.PIX,
                        new BigDecimal("12345678901234.00"),
                        PaymentCategoryEnum.SERVICOS,
                        LocalDate.now().atStartOfDay(),
                        "João");

        assertThat(validator.validate(tooManyDecimals)).isNotEmpty();
        assertThat(validator.validate(tooManyIntegers)).isNotEmpty();
    }

    @Test
    void deviceUpdateDtoValidation_shouldRejectNullBudgetFeeAndInvalidDigits() {
        DeviceUpdateInputDtoRecord nullBudgetFee =
                new DeviceUpdateInputDtoRecord(
                        1,
                        DeviceStatusEnum.EM_ANDAMENTO,
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
                        DeviceStatusEnum.EM_ANDAMENTO,
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
