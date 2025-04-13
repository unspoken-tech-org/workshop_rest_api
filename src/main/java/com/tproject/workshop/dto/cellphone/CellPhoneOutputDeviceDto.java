package com.tproject.workshop.dto.cellphone;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class CellPhoneOutputDeviceDto {
  private int id;
  private String number;
  private boolean main;
}
