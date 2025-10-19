package com.tproject.workshop.validation;

import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceStatusValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation_whenDeviceStatusIsValid() {
        // Arrange
        DeviceUpdateInputDtoRecord dto = new DeviceUpdateInputDtoRecord(
                1,
                "NOVO",
                "Problema no dispositivo",
                "Observação",
                "Orçamento",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0),
                true,
                false,
                false,
                1
        );

        // Act
        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPassValidation_whenDeviceStatusIsValidWithDifferentCase() {
        // Arrange
        DeviceUpdateInputDtoRecord dto = new DeviceUpdateInputDtoRecord(
                1,
                "em_andamento",
                "Problema no dispositivo",
                "Observação",
                "Orçamento",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0),
                true,
                false,
                false,
                1
        );

        // Act
        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidation_whenDeviceStatusIsInvalid() {
        // Arrange
        DeviceUpdateInputDtoRecord dto = new DeviceUpdateInputDtoRecord(
                1,
                "STATUS_INVALIDO",
                "Problema no dispositivo",
                "Observação",
                "Orçamento",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0),
                true,
                false,
                false,
                1
        );

        // Act
        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        ConstraintViolation<DeviceUpdateInputDtoRecord> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("deviceStatus");
        assertThat(violation.getMessage()).contains("Status do aparelho inválido");
    }

    @Test
    void shouldPassValidation_whenDeviceStatusIsEmpty() {
        // Arrange
        DeviceUpdateInputDtoRecord dto = new DeviceUpdateInputDtoRecord(
                1,
                "",
                "Problema no dispositivo",
                "Observação",
                "Orçamento",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0),
                true,
                false,
                false,
                1
        );

        // Act
        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        // Assert
        // Deve falhar por causa do @NotEmpty, não do @ValidDeviceStatus
        assertThat(violations).hasSize(1);
        ConstraintViolation<DeviceUpdateInputDtoRecord> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("deviceStatus");
        assertThat(violation.getMessage()).contains("O status do aparelho deve ser fornecido");
    }

    @Test
    void shouldPassValidation_whenDeviceStatusIsNull() {
        // Arrange
        DeviceUpdateInputDtoRecord dto = new DeviceUpdateInputDtoRecord(
                1,
                null,
                "Problema no dispositivo",
                "Observação",
                "Orçamento",
                BigDecimal.valueOf(100.0),
                BigDecimal.valueOf(50.0),
                true,
                false,
                false,
                1
        );

        // Act
        Set<ConstraintViolation<DeviceUpdateInputDtoRecord>> violations = validator.validate(dto);

        // Assert
        // Deve falhar por causa do @NotEmpty, não do @ValidDeviceStatus
        assertThat(violations).hasSize(1);
        ConstraintViolation<DeviceUpdateInputDtoRecord> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("deviceStatus");
        assertThat(violation.getMessage()).contains("O status do aparelho deve ser fornecido");
    }
}
