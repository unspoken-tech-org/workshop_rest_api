package com.tproject.workshop.dto.cellphone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants(asEnum = true)
public class CellPhoneOutputDeviceDto {
    private int id;
    private String number;
    private String name;
    private boolean main;
}
