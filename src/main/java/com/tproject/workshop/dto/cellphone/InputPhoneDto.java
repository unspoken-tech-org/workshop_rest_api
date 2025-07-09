package com.tproject.workshop.dto.cellphone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InputPhoneDto(
        Integer id,
        String name,
        @NotBlank
        @Size(min = 10, max = 11, message = "Número de telefone deve ter entre 10 e 11 dígitos")
        String number,
        boolean isPrimary
) {
}
