package com.tproject.workshop.enums;

import com.tproject.workshop.exception.BadRequestException;
import java.util.Locale;

public enum DeviceStatusEnum {
    NOVO(false, false),
    EM_ANDAMENTO(false, false),
    AGUARDANDO(false, false),
    ENTREGUE(true, true),
    DESCARTADO(true, true),
    APROVADO(true, false),
    NAO_APROVADO(true, false),
    PRONTO(true, false);

    private final boolean resetsUrgencyAndRevision;
    private final boolean closed;

    DeviceStatusEnum(boolean resetsUrgencyAndRevision, boolean closed) {
        this.resetsUrgencyAndRevision = resetsUrgencyAndRevision;
        this.closed = closed;
    }

    public boolean resetsUrgencyAndRevision() {
        return resetsUrgencyAndRevision;
    }

    public boolean isClosed() {
        return closed;
    }

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
