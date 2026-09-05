package com.tproject.workshop.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tproject.workshop.exception.BadRequestException;

public enum PaymentCategoryEnum {
    TAXA_ORCAMENTO("taxa_orcamento"),
    SERVICOS("servicos");

    private final String dbValue;

    PaymentCategoryEnum(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static PaymentCategoryEnum fromString(String value) {
        for (PaymentCategoryEnum category : values()) {
            if (category.dbValue.equalsIgnoreCase(value)) {
                return category;
            }
        }
        throw new BadRequestException(
                "Categoria de pagamento \"%s\" não é válida. Valores aceitos: taxa_orcamento, servicos", value);
    }
}
