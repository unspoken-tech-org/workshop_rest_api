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

    private final boolean resetsUrgency;
    private final boolean closed;
    private final boolean allowsPayments;

    DeviceStatusEnum(boolean resetsUrgency, boolean closed, boolean allowsPayments) {
        this.resetsUrgency = resetsUrgency;
        this.closed = closed;
        this.allowsPayments = allowsPayments;
    }

    /**
     * Statuses that clear the urgency flag. Revision is never cleared by a status change (decision
     * 7 of the Feature 24 plan).
     */
    public boolean resetsUrgency() {
        return resetsUrgency;
    }

    /** NAO_APROVADO and DESCARTADO have no warranty (decision 2). */
    public boolean allowsWarranty() {
        return this != NAO_APROVADO && this != DESCARTADO;
    }

    /** Warranty days can be edited only while the order is in progress (decision 2). */
    public boolean allowsWarrantyEdit() {
        return allowsWarranty() && this != ENTREGUE;
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
