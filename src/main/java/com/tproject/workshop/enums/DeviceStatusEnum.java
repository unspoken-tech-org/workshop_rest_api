package com.tproject.workshop.enums;

import com.tproject.workshop.exception.NotFoundException;

public enum DeviceStatusEnum {
    NOVO,
    EM_ANDAMENTO,
    AGUARDANDO,
    ENTREGUE,
    DESCARTADO,
    PRONTO;

    public static DeviceStatusEnum fromString(String status) {
        try {
            return DeviceStatusEnum.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new NotFoundException("Não foi encontrado o status: " + status);
        }
    }
} 