package com.tproject.workshop.dto.deviceStatistics;

import com.fasterxml.jackson.annotation.JsonAlias;

public record StatisticsCountDtoRecord(
    int total,
    @JsonAlias("last_month")
    int lastMonth
) {

}
