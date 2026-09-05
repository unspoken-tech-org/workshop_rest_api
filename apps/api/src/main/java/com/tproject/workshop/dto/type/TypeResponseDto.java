package com.tproject.workshop.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Device type details response")
public record TypeResponseDto(
        @Schema(example = "1")
        Integer idType,

        @Schema(example = "Lavadora")
        String type
) {
}
