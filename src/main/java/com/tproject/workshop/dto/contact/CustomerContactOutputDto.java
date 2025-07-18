package com.tproject.workshop.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@FieldNameConstants(asEnum = true)
public class CustomerContactOutputDto {

    private int id;

    private int deviceId;

    private int technicianId;

    private String technicianName;

    private String phone;

    private String type;

    private boolean hasMadeContact;

    private LocalDateTime lastContact;

    private String conversation;

    private String deviceStatus;

    @JsonProperty("lastContact")
    public String getLastContact() {
        return lastContact != null ? lastContact.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
    }
}
