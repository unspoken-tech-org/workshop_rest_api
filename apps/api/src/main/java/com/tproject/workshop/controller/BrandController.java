package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.brand.BrandResponseDto;
import com.tproject.workshop.dto.brand.BrandSearchParam;
import com.tproject.workshop.dto.brand.CreateBrandRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Brands", description = "Manage equipment brands available for device registration")
public interface BrandController {

  @Deprecated
  @Operation(
      summary = "List brands",
      description = "Returns every brand registered in the catalog, optionally filtered by a partial name. Deprecated: use POST /search instead.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
  @GetMapping
  List<BrandResponseDto> list(
      @Parameter(description = "Optional case-insensitive filter applied to the brand name")
      @RequestParam(required = false) String name);

  @Operation(
      summary = "Search brands",
      description = "Full-text fuzzy search across brand names. Supports pagination.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
  @PostMapping("/search")
  Page<BrandResponseDto> search(@RequestBody @Valid BrandSearchParam params);

  @Operation(
      summary = "Create or return existing brand",
      description = "Creates a new brand or returns an existing one if a case-insensitive match is found for the given type (idempotent).")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "201", description = "Brand created successfully")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  BrandResponseDto create(@RequestBody @Valid CreateBrandRequest request);
}
