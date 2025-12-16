package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.model.Brand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Brands", description = "Manage equipment brands available for device registration")
public interface BrandController {

  @Operation(
      summary = "List brands",
      description =
          "Returns every brand registered in the catalog, optionally filtered by a partial name "
              + "to help autocomplete and device onboarding flows.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
  @GetMapping
  List<Brand> list(
      @Parameter(description = "Optional case-insensitive filter applied to the brand name")
      @RequestParam(required = false) String name);
}
