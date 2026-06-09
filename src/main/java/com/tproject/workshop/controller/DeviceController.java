package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.device.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Devices", description = "Manage devices lifecycle from intake to delivery")
public interface DeviceController {

    @Operation(
            summary = "Filter devices",
            description = "Provides a paginated list of devices filtered by customer data, status, urgency, and catalog attributes.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Devices filtered successfully")
    @PostMapping("/filter")
    Page<DeviceTableDto> list(@RequestBody(required = false) @Valid DeviceQueryParam deviceQueryParam);

    @Operation(
            summary = "Find device by id",
            description = "Returns the full device details including customer info and status timeline for service desks.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Device retrieved successfully")
    @GetMapping("/{deviceId}")
    DeviceOutputDto findOne(@Parameter(description = "Device identifier") @PathVariable("deviceId") int deviceId);

    @Operation(
            summary = "Update device",
            description = "Updates device status, pricing details, and ownership data during the repair lifecycle.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Device updated successfully")
    @PutMapping("/update")
    DeviceOutputDto update(@RequestBody DeviceUpdateInputDtoRecord device);

    @Operation(
            summary = "Update device status",
            description = "Updates only the device status. Resets urgency and revision for terminal statuses, auto-sets departure date when delivered or discarded.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Device status updated successfully")
    @PutMapping("/update/{deviceId}/status")
    DeviceOutputDto updateStatus(
            @Parameter(description = "Device identifier") @PathVariable("deviceId") int deviceId,
            @RequestBody @Valid DeviceStatusInputRecord dto);

    @Operation(
            summary = "Update device urgency",
            description = "Toggles the urgency flag. Reverts status to NOVO if the device is currently delivered or discarded.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Device urgency updated successfully")
    @PutMapping("/update/{deviceId}/urgency")
    DeviceOutputDto updateUrgency(
            @Parameter(description = "Device identifier") @PathVariable("deviceId") int deviceId,
            @RequestBody @Valid DeviceUrgencyInputRecord dto);

    @Operation(
            summary = "Update device revision",
            description = "Toggles the revision flag. Reverts status to NOVO if the device is currently delivered or discarded.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Device revision updated successfully")
    @PutMapping("/update/{deviceId}/revision")
    DeviceOutputDto updateRevision(
            @Parameter(description = "Device identifier") @PathVariable("deviceId") int deviceId,
            @RequestBody @Valid DeviceRevisionInputRecord dto);

    @Operation(
            summary = "Create device",
            description = "Registers a new device intake with catalog mapping, color selection, and urgency flags.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "201", description = "Device created successfully")
    @PostMapping("/create")
    @ResponseStatus(code = HttpStatus.CREATED)
    CreateDeviceOutputDtoRecord create(@RequestBody DeviceInputDtoRecord device);
}
