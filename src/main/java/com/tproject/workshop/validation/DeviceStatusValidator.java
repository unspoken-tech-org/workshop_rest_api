package com.tproject.workshop.validation;

import com.tproject.workshop.enums.DeviceStatusEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DeviceStatusValidator implements ConstraintValidator<ValidDeviceStatus, String> {

    @Override
    public void initialize(ValidDeviceStatus constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        try {
            DeviceStatusEnum.fromString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
