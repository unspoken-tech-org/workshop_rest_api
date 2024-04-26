package com.tproject.workshop.dto.device;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
public class DeviceOutputDto {
    private int id;
    private int customerId;
    private int deviceStatusId;
    private int brandId;
    private int modelId;
    private int typeId;
    private int technicianId;
    private String problem;
    private String observation;
    private boolean hasUrgency;
}
