package com.tproject.workshop.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Model details response")
public record ModelResponseDto(
    @Schema(example = "100")
    Integer idModel,
    @Schema(example = "WT12345")
    String model
) {}
