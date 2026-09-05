package com.tproject.workshop.dto.device;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@FieldNameConstants(asEnum = true)
@Data
public class MinifiedDeviceTableOutputDto {

  private int deviceId;
  private int customerId;
  private String typeBrandModel;
  private String deviceStatus;
  private String problem;
  private boolean hasUrgency;
  private boolean revision;
  private LocalDateTime entryDate;
  private LocalDateTime departureDate;
}
