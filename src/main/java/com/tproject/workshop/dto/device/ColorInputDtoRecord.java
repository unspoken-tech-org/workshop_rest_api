package com.tproject.workshop.dto.device;

import jakarta.validation.constraints.NotBlank;

public record ColorInputDtoRecord(
                Integer idColor,
                @NotBlank(message = "A cor é obrigatória") String color) {
}
