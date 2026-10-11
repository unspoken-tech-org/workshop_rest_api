package com.tproject.workshop.dto.deviceStatistics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tproject.workshop.dto.device.DeviceTableDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record DeviceStatisticsOutputDtoRecord(
        @Schema(description = "Count of devices with status NOVO", example = "{\"total\":5,\"last_month\":2}")
        @JsonProperty("NOVO")
        StatisticsCountDtoRecord novo,
        @Schema(description = "Count of devices in progress (EM_ANDAMENTO)", example = "{\"total\":12,\"last_month\":4}")
        @JsonProperty("EM_ANDAMENTO")
        StatisticsCountDtoRecord emAndamento,
        @Schema(description = "Count of devices waiting for action (AGUARDANDO)", example = "{\"total\":7,\"last_month\":3}")
        @JsonProperty("AGUARDANDO")
        StatisticsCountDtoRecord aguardando,
        @Schema(description = "Count of devices already delivered (ENTREGUE)", example = "{\"total\":18,\"last_month\":6}")
        @JsonProperty("ENTREGUE")
        StatisticsCountDtoRecord entregue,
        @Schema(description = "Count of devices ready for pickup (PRONTO)", example = "{\"total\":9,\"last_month\":2}")
        @JsonProperty("PRONTO")
        StatisticsCountDtoRecord pronto,
        @Schema(description = "Count of discarded devices (DESCARTADO)", example = "{\"total\":1,\"last_month\":0}")
        @JsonProperty("DESCARTADO")
        StatisticsCountDtoRecord descartado,
        @Schema(description = "Count of devices under revision (REVISAO)", example = "{\"total\":3,\"last_month\":1}")
        @JsonProperty("REVISAO")
        StatisticsCountDtoRecord revisao,
        @Schema(description = "Count of devices flagged as urgent (URGENTE)", example = "{\"total\":2,\"last_month\":1}")
        @JsonProperty("URGENTE")
        StatisticsCountDtoRecord urgente,
        @Schema(description = "Count of approved devices (APROVADO)", example = "{\"total\":15,\"last_month\":5}")
        @JsonProperty("APROVADO")
        StatisticsCountDtoRecord aprovado,
        @Schema(description = "Count of devices not approved (NAO_APROVADO)", example = "{\"total\":4,\"last_month\":1}")
        @JsonProperty("NAO_APROVADO")
        StatisticsCountDtoRecord naoAprovado,
        @Schema(
                description = "Recent devices viewed by operators with their summary",
                example = "[{\"deviceId\":101,\"customerName\":\"John Doe\",\"status\":\"EM_ANDAMENTO\"}]")
        @JsonProperty("last_viewed_devices")
        List<DeviceTableDto> lastViewedDevices
) {

}
