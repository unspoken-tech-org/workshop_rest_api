package com.tproject.workshop.dto.device;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
@AllArgsConstructor
public class DeviceOutputDto {
    private int deviceId;
    private int customerId;
    private String customerName;
    private String deviceStatus;
    private String brandName;
    private String modelName;
    private String typeName;
    private int technicianId;
    private String technicianName;
    private String problem;
    private String observation;
    private List<String> deviceColors;
    private boolean hasUrgency;
    private boolean isRevision;
}
