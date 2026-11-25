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
@Min(value = PageBounds.MIN_PAGE, message = "O número da página deve ser maior ou igual a zero")
@Max(
        value = PageBounds.MAX_PAGE,
        message = "O número da página deve ser no máximo " + PageBounds.MAX_PAGE)
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPageNumber {

    String message() default "Número da página inválido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
