package com.tproject.workshop.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tproject.workshop.exception.BadRequestException;
import java.util.Locale;

public enum DeviceStatusEnum {
    NOVO(false, false, false),
    EM_ANDAMENTO(false, false, true),
    AGUARDANDO(false, false, true),
    ENTREGUE(true, true, true),
    DESCARTADO(true, true, false),
    APROVADO(true, false, true),
    NAO_APROVADO(true, false, true),
    PRONTO(true, false, true);

    private final boolean resetsUrgencyAndRevision;
    private final boolean closed;
    private final boolean allowsPayments;

    DeviceStatusEnum(boolean resetsUrgencyAndRevision, boolean closed, boolean allowsPayments) {
        this.resetsUrgencyAndRevision = resetsUrgencyAndRevision;
        this.closed = closed;
        this.allowsPayments = allowsPayments;
    }

    public boolean resetsUrgencyAndRevision() {
        return resetsUrgencyAndRevision;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean allowsPayments() {
        return allowsPayments;
    }

    public boolean allowsPayment(PaymentCategoryEnum category) {
        if (this == DESCARTADO) {
            return false;
        }
        if (this == NOVO) {
            return category == PaymentCategoryEnum.TAXA_ORCAMENTO;
        }
        return true;
    }

    @JsonValue
    public String getValue() {
        return name();
    }

    @JsonCreator
    public static DeviceStatusEnum fromString(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Não foi encontrado o status: " + status);
        }
        try {
            return DeviceStatusEnum.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Não foi encontrado o status: " + status);
        }
    }
}
