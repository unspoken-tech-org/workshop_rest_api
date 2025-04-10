package com.tproject.workshop.dto.device;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.Timestamp;
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
    private String budget;
    private List<String> deviceColors;
    private boolean hasUrgency;
    private boolean isRevision;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Timestamp entryDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Timestamp departureDate;
    private Timestamp lastUpdate;
}
