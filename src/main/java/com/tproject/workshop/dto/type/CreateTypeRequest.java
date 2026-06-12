package com.tproject.workshop.dto.type;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTypeRequest(
    @Schema(example = "Lavadora Industrial")
    String type
) {}
