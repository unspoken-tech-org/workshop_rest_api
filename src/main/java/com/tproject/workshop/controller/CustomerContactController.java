package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.contact.CustomerContactInputDto;
import com.tproject.workshop.model.CustomerContact;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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
  CustomerContact save(@RequestBody CustomerContactInputDto contact);
}
