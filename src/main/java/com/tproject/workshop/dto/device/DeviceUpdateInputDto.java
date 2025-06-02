package com.tproject.workshop.dto.device;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeviceUpdateInputDto {
    private int deviceId;
    private String deviceStatus;
    private String problem;
    private String observation;
    private String budget;
    private BigDecimal laborValue;
    private BigDecimal serviceValue;
}
