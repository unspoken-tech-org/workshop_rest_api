package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.payment.PaymentDeviceInputDto;
import com.tproject.workshop.dto.payment.PaymentResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Payments", description = "Capture payments associated with device services and repairs")
public interface PaymentController {

    @Operation(
            summary = "Register payment for a device",
            description = "Creates a payment entry linked to a device, keeping billing and cash-flow data consistent.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "201", description = "Payment registered successfully")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PaymentResponseDto save(@RequestBody PaymentDeviceInputDto payment);
}
