package com.tproject.workshop.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DeviceStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDeviceStatus {
    String message() default "Status do aparelho inválido. Valores aceitos: NOVO, EM_ANDAMENTO, AGUARDANDO, ENTREGUE, DESCARTADO, APROVADO, NAO_APROVADO, PRONTO";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
