package com.tproject.workshop.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class CustomerContactOutputDto {

  private int id;

  private int deviceId;

  private int technicianId;

  private int phoneId;

  private String type;

  private String callStatus;

  private LocalDateTime lastContact;

  private String conversation;

  private String deviceStatus;

  @JsonProperty("lastContact")
  public String getLastContact() {
    return lastContact != null ? lastContact.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
  }
}
