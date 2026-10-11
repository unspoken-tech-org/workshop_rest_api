package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.model.CreateModelRequest;
import com.tproject.workshop.dto.model.ModelResponseDto;
import com.tproject.workshop.dto.model.ModelSearchParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Models", description = "Manage device models available for device registration")
public interface ModelController {

    @Operation(
        summary = "Search models",
        description = "Full-text fuzzy search across model names, filtered by brand. Supports pagination.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    @PostMapping("/search")
    Page<ModelResponseDto> search(@RequestBody @Valid ModelSearchParam params);

    @Operation(
        summary = "Create or return existing model",
        description = "Creates a new model or returns an existing one if a case-insensitive match is found for the given brand (idempotent).")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "201", description = "Model created successfully")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ModelResponseDto create(@RequestBody @Valid CreateModelRequest request);
}
