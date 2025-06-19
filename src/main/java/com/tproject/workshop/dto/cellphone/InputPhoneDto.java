package com.tproject.workshop.dto.cellphone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InputPhoneDto(
        Integer id,
        @NotBlank
        @Size(min = 3, max = 50)
        String name,
        @NotBlank
        @Size(min = 10, max = 11)
        String number,
        @NotNull
        boolean isPrimary
) {
}
