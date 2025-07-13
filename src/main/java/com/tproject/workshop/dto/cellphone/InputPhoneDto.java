package com.tproject.workshop.dto.cellphone;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tproject.workshop.config.jackson.PhoneNumberDeserializer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InputPhoneDto(
        Integer id,
        String name,
        @NotBlank(message = "Número de telefone não pode ser vazio")
        @Size(min = 10, max = 11, message = "Número de telefone deve ter entre 10 e 11 dígitos")
        @JsonDeserialize(using = PhoneNumberDeserializer.class)
        String number,
        boolean isPrimary
) {
}
