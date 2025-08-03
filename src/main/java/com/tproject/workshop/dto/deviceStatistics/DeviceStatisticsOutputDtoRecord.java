package com.tproject.workshop.dto.deviceStatistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tproject.workshop.dto.device.DeviceTableDto;

import java.util.List;

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
        @JsonProperty("APROVADO")
        StatisticsCountDtoRecord aprovado,
        @JsonProperty("NAO_APROVADO")
        StatisticsCountDtoRecord naoAprovado,
        @JsonProperty("last_viewed_devices")
        List<DeviceTableDto> lastViewedDevices
) {

}
