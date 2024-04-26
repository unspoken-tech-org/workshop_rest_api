package com.tproject.workshop.dto.device;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;
import java.util.List;

@Data
@FieldNameConstants(asEnum = true)
public class DeviceQueryParam {

    List<Integer> deviceIds;

    String customerName;

    Integer customerId;

    Integer typeId;

    Integer brandId;

    Integer modelId;

    Integer statusId;

    Timestamp entryDate;

    Timestamp departureDate;
}
