package com.tproject.workshop.dto.contact;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tproject.workshop.config.jackson.PhoneNumberDeserializer;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerContactInputDto(
        @NotNull(message = "O ID do dispositivo é obrigatório")
        Integer deviceId,
        String contactType,
        @NotNull(message = "O ID do técnico é obrigatório")
        Integer technicianId,
        @Size(min = 10, max = 11, message = "O número de telefone, se preenchido, deve ter no mínimo 10 caracteres e no máximo 11")
        @JsonDeserialize(using = PhoneNumberDeserializer.class)
        String phoneNumber,
        @NotEmpty(message = "A mensagem é obrigatória")
        String message,
        @NotNull(message = "O status do contato é obrigatório")
        Boolean contactStatus,
        @NotNull(message = "O status do dispositivo é obrigatório")
        String deviceStatus,
        @NotNull(message = "A data do contato é obrigatória")
        String contactDate
) {
}
