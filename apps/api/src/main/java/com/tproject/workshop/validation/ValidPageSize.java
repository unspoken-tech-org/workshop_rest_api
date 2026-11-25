package com.tproject.workshop.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Min(value = PageBounds.MIN_SIZE, message = "O tamanho da página deve ser maior que zero")
@Max(
        value = PageBounds.MAX_SIZE,
        message = "O tamanho da página deve ser no máximo " + PageBounds.MAX_SIZE)
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPageSize {

    String message() default "Tamanho da página inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
