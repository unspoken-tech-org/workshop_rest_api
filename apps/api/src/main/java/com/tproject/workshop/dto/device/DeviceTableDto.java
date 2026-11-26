package com.tproject.workshop.dto.device;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.tproject.workshop.enums.DeviceStatusEnum;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

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
    private DeviceStatusEnum status;
    private String problem;
    private String observation;

    @JsonAlias("has_urgency")
    private boolean hasUrgency;

    @JsonAlias("has_revision")
    private boolean hasRevision;

    @JsonAlias("entry_date")
    private LocalDateTime entryDate;

    @JsonAlias("departure_date")
    private LocalDateTime departureDate;
}
