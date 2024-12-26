package com.tproject.workshop.dto.device;

import java.util.List;
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

    List<Integer> typeIds = List.of();

    List<String> status = List.of();

    List<Integer> deviceTypes = List.of();

    Integer modelId;

    Integer brandId;

    Timestamp entryDate;

    Timestamp departureDate;
}
