package com.tproject.workshop.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tproject.workshop.exception.BadRequestException;

public enum PaymentMethodEnum {
    CREDITO("credito"),
    DEBITO("debito"),
    DINHEIRO("dinheiro"),
    PIX("pix"),
    OUTRO("outro");

    private final String dbValue;

    PaymentMethodEnum(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static PaymentMethodEnum fromString(String value) {
        for (PaymentMethodEnum method : values()) {
            if (method.dbValue.equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new BadRequestException(
                "Tipo de pagamento \"%s\" não é válido. Valores aceitos: credito, debito, dinheiro, pix, outro", value);
    }
}
