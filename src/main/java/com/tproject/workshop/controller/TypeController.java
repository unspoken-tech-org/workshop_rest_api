package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.model.Type;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Types", description = "Expose device types for classification and catalog management")
public interface TypeController {

  @Operation(
      summary = "List device types",
      description =
          "Returns all device types configured in the catalog, optionally filtered by name to speed up selection.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Types retrieved successfully")
  @GetMapping
  List<Type> findAll(
      @Parameter(description = "Optional case-insensitive filter applied to the type name")
      @RequestParam(required = false) String name);
}
