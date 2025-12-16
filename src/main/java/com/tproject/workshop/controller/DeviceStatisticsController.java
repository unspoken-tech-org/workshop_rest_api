package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Device Statistics", description = "Operational dashboards for device pipeline and recent activity")
public interface DeviceStatisticsController {


    @Operation(
            summary = "Get device statistics",
            description = "Provides aggregated counts per device status and the latest viewed devices to guide workload balancing.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Statistics generated successfully")
    @GetMapping
    DeviceStatisticsOutputDtoRecord getDeviceStatistics();
}
