package com.tproject.workshop.dto.color;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Color details response")
public record ColorResponseDto(
    @Schema(example = "1")
    Integer idColor,
    
    @Schema(example = "Preto")
    String color
) {}
