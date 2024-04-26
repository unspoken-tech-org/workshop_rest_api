package com.tproject.workshop.dto.device;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class DeviceInputDto {
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

