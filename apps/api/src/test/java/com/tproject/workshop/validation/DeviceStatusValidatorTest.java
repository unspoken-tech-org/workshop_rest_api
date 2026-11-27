package com.tproject.workshop.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.dto.device.DeviceInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import com.tproject.workshop.enums.DeviceStatusEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeviceStatusValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation_whenDeviceStatusIsValid() {
        DeviceUpdateInputDtoRecord dto =
                new DeviceUpdateInputDtoRecord(
                        1,
                        DeviceStatusEnum.NOVO,
                        "Problema no dispositivo",
                        "Observação",
                        "Orçamento",
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(50.0),
                        false,
                        false,
                        1,
                        null,
                        null);

        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidation_whenDeviceStatusIsNull() {
        DeviceUpdateInputDtoRecord dto =
                new DeviceUpdateInputDtoRecord(
                        1,
                        null,
                        "Problema no dispositivo",
                        "Observação",
                        "Orçamento",
                        BigDecimal.valueOf(100.0),
                        BigDecimal.valueOf(50.0),
                        false,
                        false,
                        1,
                        null,
                        null);

        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        ConstraintViolation<DeviceUpdateInputDtoRecord> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("deviceStatus");
        assertThat(violation.getMessage()).contains("O status do aparelho deve ser fornecido");
    }

    @Test
    void shouldRejectBlankOrNullContactType() {
        CustomerContactInputDto dto =
                new CustomerContactInputDto(
                        1,
                        "   ",
                        2,
                        null,
                        "Mensagem",
                        true,
                        DeviceStatusEnum.EM_ANDAMENTO,
                        "2026-05-01T10:00:00");

        Set<ConstraintViolation<CustomerContactInputDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("contactType");
    }

    @Test
    void shouldRejectBlankColorsInsideList() {
        DeviceInputDtoRecord dto =
                new DeviceInputDtoRecord(
                        1,
                        new TypeBrandModelInputDtoRecord("Celular", "Samsung", "A54"),
                        null,
                        "Tela quebrada",
                        null,
                        BigDecimal.TEN,
                        List.of("azul", "   "),
                        false,
                        null,
                        false);

        Set<ConstraintViolation<DeviceInputDtoRecord>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().startsWith("colors"));
    }
}
