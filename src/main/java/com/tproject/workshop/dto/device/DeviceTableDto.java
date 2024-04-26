package com.tproject.workshop.dto.device;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;

@Data
@FieldNameConstants(asEnum = true)
public class DeviceTableDto {
    private int deviceId;
    private int customerId;
    private String customerName;
    private String type;
    private String brand;
    private String model;
    private String status;
    private String problem;
    private String observation;
    private boolean hasUrgency;
    private Timestamp entryDate;
    private Timestamp departureDate;
}
