package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.dto.payment.PaymentResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE')")
@Tag(
        name = "Payments",
        description = "Capture payments associated with device services and repairs")
public interface PaymentController {

    @Operation(
            summary = "Register payment for a device",
            description =
                    "Creates a payment entry linked to a device, keeping billing and cash-flow data consistent.")
    @ApiGlobalResponses
    @Parameter(
            name = "Idempotency-Key",
            in = ParameterIn.HEADER,
            required = false,
            description =
                    "Optional. Repeating the request with the same key and body returns the first"
                            + " response without creating another payment (24h).")
    @ApiResponse(responseCode = "201", description = "Payment registered successfully")
    @ApiResponse(
            responseCode = "409",
            description = "A request with the same Idempotency-Key is still being processed")
    @ApiResponse(
            responseCode = "422",
            description = "Idempotency-Key reused with a different request body")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponseDto create(@RequestBody PaymentDeviceInputDto payment);
}
