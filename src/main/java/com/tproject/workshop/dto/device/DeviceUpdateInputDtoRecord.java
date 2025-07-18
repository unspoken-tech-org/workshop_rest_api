package com.tproject.workshop.dto.device;

import java.math.BigDecimal;

public record DeviceUpdateInputDtoRecord(
        int deviceId,
        String deviceStatus,
        String problem,
        String observation,
        String budget,
        BigDecimal laborValue,
        BigDecimal serviceValue,
        boolean laborValueCollected,
        boolean hasUrgency,
        boolean revision,
        Integer technicianId
) {

}
