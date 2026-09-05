package com.tproject.workshop.dto.cellphone;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tproject.workshop.config.jackson.PhoneNumberDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InputPhoneDto(
        @Schema(description = "Phone identifier used when updating existing records", example = "10")
        Integer id,
        @Schema(description = "Friendly label displayed to operators", example = "Mobile")
        String name,
        @Schema(description = "Sanitized phone number with 10 to 11 digits", example = "11999991111")
        @NotBlank(message = "Número de telefone não pode ser vazio")
        @Size(min = 10, max = 11, message = "Número de telefone deve ter entre 10 e 11 dígitos")
        @JsonDeserialize(using = PhoneNumberDeserializer.class)
        String number,
        @Schema(description = "Marks the main phone to be used as the first contact option", example = "true")
        boolean isPrimary
) {
}
