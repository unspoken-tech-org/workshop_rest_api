package com.tproject.workshop.dto.contact;

import lombok.Data;

@Data
public class CustomerContactInputDto {
  private Integer deviceId;
  private String contactType;
  private Integer technicianId;
  private Integer phoneNumberId;
  private String message;
  private Boolean contactStatus;
  private String deviceStatus;
  private String contactDate;

}
