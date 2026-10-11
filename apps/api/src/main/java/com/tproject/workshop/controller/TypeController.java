package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.type.CreateTypeRequest;
import com.tproject.workshop.dto.type.TypeResponseDto;
import com.tproject.workshop.dto.type.TypeSearchParam;
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

@Tag(name = "Types", description = "Expose device types for classification and catalog management")
public interface TypeController {

  @Deprecated
  @Operation(
      summary = "List device types",
      description = "Returns all device types configured in the catalog, optionally filtered by name to speed up selection. Deprecated: use POST /search instead.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Types retrieved successfully")
  @GetMapping
  List<TypeResponseDto> findAll(
      @Parameter(description = "Optional case-insensitive filter applied to the type name")
      @RequestParam(required = false) String name);

  @Operation(
      summary = "Search types",
      description = "Full-text fuzzy search across type names. Supports pagination.")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
  @PostMapping("/search")
  Page<TypeResponseDto> search(@RequestBody @Valid TypeSearchParam params);

  @Operation(
      summary = "Create or return existing type",
      description = "Creates a new type or returns an existing one if a case-insensitive match is found (idempotent).")
  @ApiGlobalResponses
  @ApiResponse(responseCode = "201", description = "Type created successfully")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  TypeResponseDto create(@RequestBody @Valid CreateTypeRequest request);
}
