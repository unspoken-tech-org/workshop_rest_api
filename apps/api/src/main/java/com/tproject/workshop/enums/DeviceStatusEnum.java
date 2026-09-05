package com.tproject.workshop.enums;

import com.tproject.workshop.exception.BadRequestException;

public enum DeviceStatusEnum {
    NOVO,
    EM_ANDAMENTO,
    AGUARDANDO,
    ENTREGUE,
    DESCARTADO,
    APROVADO,
    NAO_APROVADO,
    PRONTO;

    public static DeviceStatusEnum fromString(String status) {
        try {
            return DeviceStatusEnum.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Não foi encontrado o status: " + status);
        }
    }
} 