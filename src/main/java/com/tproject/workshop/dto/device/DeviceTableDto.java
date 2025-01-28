package com.tproject.workshop.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Timestamp entryDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Timestamp departureDate;
}
