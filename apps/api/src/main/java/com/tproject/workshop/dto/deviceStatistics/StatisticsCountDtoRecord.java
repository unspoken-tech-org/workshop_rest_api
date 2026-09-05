package com.tproject.workshop.dto.deviceStatistics;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatisticsCountDtoRecord(
    @Schema(description = "Total number of devices for the status", example = "42")
    int total,
    @Schema(description = "Devices counted in the last month for the status", example = "8")
    @JsonAlias("last_month")
    int lastMonth
) {

}
