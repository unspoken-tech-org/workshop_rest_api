package com.tproject.workshop.dto.device;

import com.tproject.workshop.utils.filter_utils.Ordenation;
import com.tproject.workshop.utils.filter_utils.OrderByDirection;
import com.tproject.workshop.validation.PageBounds;
import com.tproject.workshop.validation.ValidPageNumber;
import com.tproject.workshop.validation.ValidPageSize;
import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

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

    @ValidPageNumber int page = PageBounds.DEFAULT_PAGE;

    @ValidPageSize int size = PageBounds.DEFAULT_SIZE;

    Ordenation ordenation = new Ordenation("entry_date", OrderByDirection.DESC);
}
