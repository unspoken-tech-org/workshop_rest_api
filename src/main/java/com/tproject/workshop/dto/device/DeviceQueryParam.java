package com.tproject.workshop.dto.device;

import java.util.List;

import com.tproject.workshop.utils.filter_utils.Ordenation;
import com.tproject.workshop.utils.filter_utils.OrderByDirection;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

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

    List<Integer> deviceBrands = List.of();

    boolean urgency = false;

    boolean revision = false;

    String initialEntryDate;

    String finalEntryDate;

    int page = 0;
    
    int size = 15;

    Ordenation ordenation = new Ordenation("entry_date", OrderByDirection.DESC);
}
