package com.tproject.workshop.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateModelRequest(
    @Schema(example = "WT500X")
    String model
) {}
