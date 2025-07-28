package com.tproject.workshop.dto.device;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tproject.workshop.config.jackson.CustomDateSerializer;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;

@Data
@FieldNameConstants(asEnum = true)
public class DeviceTableDto {
    @JsonAlias("device_id")
    private int deviceId;
    @JsonAlias("customer_id")
    private int customerId;
    @JsonAlias("customer_name")
    private String customerName;
    private String type;
    private String brand;
    private String model;
    private String status;
    private String problem;
    private String observation;
    @JsonAlias("has_urgency")
    private boolean hasUrgency;
    @JsonAlias("has_revision")
    private boolean hasRevision;
    @JsonAlias("entry_date")
    @JsonSerialize(using = CustomDateSerializer.class)
    private LocalDateTime entryDate;
    @JsonAlias("departure_date")
    @JsonSerialize(using = CustomDateSerializer.class)
    private LocalDateTime departureDate;
}
