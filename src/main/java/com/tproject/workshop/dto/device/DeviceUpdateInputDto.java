package com.tproject.workshop.dto.device;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DeviceUpdateInputDto {
  private int deviceId;
  private String deviceStatus;
  private String problem;
  private String observation;
  private String budget;
//  private BigDecimal laborValue;
}
