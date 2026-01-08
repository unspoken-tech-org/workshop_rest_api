package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.apikey.ApiKeyCreatedResponse;
import com.tproject.workshop.dto.apikey.ApiKeyResponse;
import com.tproject.workshop.dto.apikey.CreateApiKeyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "API Keys", description = "Administrative endpoints for API Key management")
public interface ApiKeyController {

    @Operation(
            summary = "Create API Key",
            description = "Creates a new API Key for a client on a specific platform. " +
                    "The full key value is only returned once at creation time.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "201", description = "API Key created successfully")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ApiKeyCreatedResponse create(@Valid @RequestBody CreateApiKeyRequest request);

    @Operation(
            summary = "List all API Keys",
            description = "Returns all API Keys with masked key values for security.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "API Keys retrieved successfully")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<ApiKeyResponse> listAll();

    @Operation(
            summary = "Find API Key by ID",
            description = "Returns a single API Key by its identifier with masked key value.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "API Key retrieved successfully")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiKeyResponse findById(@Parameter(description = "API Key identifier") @PathVariable Long id);

    @Operation(
            summary = "Revoke API Key",
            description = "Deactivates an API Key. The key will no longer be usable for authentication.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "204", description = "API Key revoked successfully")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void revoke(@Parameter(description = "API Key identifier") @PathVariable Long id);
}
