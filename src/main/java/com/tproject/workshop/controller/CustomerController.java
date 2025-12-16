package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Customers", description = "Manage customers, their contact details, and device ownership records")
public interface CustomerController {

    @Operation(
            summary = "Search customers",
            description = "Performs a paginated search combining personal data and phone filters to speed up desk lookup.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Customers retrieved successfully")
    @PostMapping("/search")
    Page<CustomerListOutputDto> search(@RequestBody(required = false) @Valid CustomerFilterDto filters);

    @Operation(
            summary = "Find customer by id",
            description = "Returns a customer with full profile, phones, and devices to support service desk workflows.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Customer retrieved successfully")
    @GetMapping("/{id}")
    CustomerOutputDto findById(@Parameter(description = "Customer identifier") @PathVariable("id") int idCostumer);

    /**
     * Temporary endpoint for JPA vs JDBC benchmark comparison.
     * Returns the same data as findById but uses JPA instead of native SQL.
     */
    @Operation(
            summary = "Find customer by id using JPA",
            description = "Benchmark endpoint that fetches the same customer data via JPA to compare performance with native SQL.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Customer retrieved successfully with JPA")
    @GetMapping("/{id}/jpa")
    CustomerOutputDto findByIdJpa(@Parameter(description = "Customer identifier") @PathVariable("id") int id);

    @Operation(
            summary = "Create customer",
            description = "Registers a new customer with validated phones to enable device intake and future contact.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CustomerOutputDto create(@RequestBody @NotNull @Valid InputCustomerDtoRecord inputCustomerDto);

    @Operation(
            summary = "Update customer",
            description = "Updates customer core information and phone list to keep communication channels accurate.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    CustomerOutputDto update(
            @Parameter(description = "Customer identifier") @PathVariable("id") int id,
            @RequestBody @NotNull @Valid InputCustomerDtoRecord inputCustomerDto);
}
