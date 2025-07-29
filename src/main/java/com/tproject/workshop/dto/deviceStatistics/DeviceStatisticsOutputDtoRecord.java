package com.tproject.workshop.dto.deviceStatistics;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tproject.workshop.dto.device.DeviceTableDto;

public record DeviceStatisticsOutputDtoRecord(
    @JsonProperty("NOVO")
    StatisticsCountDtoRecord novo,
    @JsonProperty("EM_ANDAMENTO")
    StatisticsCountDtoRecord emAndamento,
    @JsonProperty("AGUARDANDO")
    StatisticsCountDtoRecord aguardando,
    @JsonProperty("ENTREGUE")
    StatisticsCountDtoRecord entregue,
    @JsonProperty("PRONTO")
    StatisticsCountDtoRecord pronto,
    @JsonProperty("DESCARTADO")
    StatisticsCountDtoRecord descartado,
    @JsonProperty("REVISAO")
    StatisticsCountDtoRecord revisao,
    @JsonProperty("URGENTE")
    StatisticsCountDtoRecord urgente,
    @JsonProperty("last_viewed_devices")
    List<DeviceTableDto> lastViewedDevices
) {
   
}
