package com.tproject.workshop.dto.device;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;

@Data
@FieldNameConstants(asEnum = true)
public class DeviceQueryParam {

    Integer deviceId;

    String customerName;

    String customerPhone;

    String customerCpf;

    Integer typeId;

    Integer brandId;

    Integer modelId;

    Integer statusId;

    Timestamp entryDate;

    Timestamp departureDate;
}
