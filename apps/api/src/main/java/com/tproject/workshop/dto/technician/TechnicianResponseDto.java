package com.tproject.workshop.dto.technician;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Technician details response")
public record TechnicianResponseDto(
        @Schema(example = "1")
        Integer id,

        @Schema(example = "John Doe")
        String name,

        @Schema(example = "44988776655")
        String number
) {
}
