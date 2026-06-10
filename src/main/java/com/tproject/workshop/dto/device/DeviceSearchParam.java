package com.tproject.workshop.dto.device;

import com.tproject.workshop.utils.filter_utils.Ordenation;
import com.tproject.workshop.utils.filter_utils.OrderByDirection;
import jakarta.validation.constraints.Min;

import java.util.List;

public record DeviceSearchParam(
    String searchQuery,

    Integer deviceId,

    String customerPhone,

    String customerCpf,

    List<String> status,

    Boolean urgency,

    Boolean revision,

    String initialEntryDate,

    String finalEntryDate,

    @Min(value = 0, message = "O número da página deve ser maior ou igual a zero")
    Integer page,

    @Min(value = 1, message = "O tamanho da página deve ser maior que zero")
    Integer size,

    Ordenation ordenation
) {
    public DeviceSearchParam {
        if (status == null) status = List.of();
        if (page == null) page = 0;
        if (size == null) size = 15;
        if (ordenation == null) ordenation = new Ordenation("entry_date", OrderByDirection.DESC);
        if (searchQuery != null && searchQuery.isBlank()) searchQuery = null;
    }
}
