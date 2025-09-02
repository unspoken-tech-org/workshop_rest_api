package com.tproject.workshop.dto.device;

import com.tproject.workshop.utils.filter_utils.Ordenation;
import com.tproject.workshop.utils.filter_utils.OrderByDirection;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@FieldNameConstants(asEnum = true)
public class DeviceQueryParam {

    Integer deviceId;

    String customerName;

    String customerPhone;

    String customerCpf;

    List<String> status = List.of();

    List<Integer> deviceTypes = List.of();

    List<Integer> deviceBrands = List.of();

    Boolean urgency;

    Boolean revision;

    String initialEntryDate;

    String finalEntryDate;

    @Min(value = 0, message = "O número da página deve ser maior ou igual a zero")
    int page = 0;

    @Min(value = 1, message = "O tamanho da página deve ser maior que zero")
    int size = 15;

    Ordenation ordenation = new Ordenation("entry_date", OrderByDirection.DESC);
}
