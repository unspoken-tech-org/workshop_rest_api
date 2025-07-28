package com.tproject.workshop.dto.deviceStatistics;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tproject.workshop.dto.device.DeviceTableDto;

public record DeviceStatisticsOutputDtoRecord(
    StatisticsCountDtoRecord novo,
    @JsonProperty("em_andamento")
    StatisticsCountDtoRecord emAndamento,
    StatisticsCountDtoRecord aguardando,
    StatisticsCountDtoRecord entregue,
    StatisticsCountDtoRecord pronto,
    StatisticsCountDtoRecord descartado,
    StatisticsCountDtoRecord revisao,
    StatisticsCountDtoRecord urgente,
    @JsonProperty("last_viewed_devices")
    List<DeviceTableDto> lastViewedDevices
) {
   
}
