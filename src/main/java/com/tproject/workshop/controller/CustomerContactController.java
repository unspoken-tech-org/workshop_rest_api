package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.dto.contact.CustomerContactOutputDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Customer Contacts", description = "Track customer outreach and technician follow-ups for devices")
public interface CustomerContactController {

  @Operation(
      summary = "Log customer contact",
      description =
          "Registers a new customer contact entry, linking a technician, device, and the message "
              + "left for follow-up or status updates.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "201", description = "Contact registered successfully")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  CustomerContactOutputDto save(@Valid @RequestBody CustomerContactInputDto contact);

  @Operation(
      summary = "Update customer contact",
      description =
          "Updates an existing customer contact entry, changing the message, type, phone, status, "
              + "and optionally updating the device status.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Contact updated successfully")
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  CustomerContactOutputDto update(
      @Parameter(description = "Contact identifier") @PathVariable("id") int id,
      @Valid @RequestBody CustomerContactInputDto contact);
}
