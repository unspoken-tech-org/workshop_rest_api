package com.tproject.workshop.dto.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

  @JsonProperty("entryDate")
  public String getEntryDate() {
    return entryDate != null ? entryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
  }
  @JsonProperty("departureDate")
  public String getDepartureDate() {
    return departureDate != null ? departureDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
  }
}
