package com.tproject.workshop.dto.contact;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CustomerContactInputDto(
        @NotNull(message = "O ID do dispositivo é obrigatório")
        Integer deviceId,
        String contactType,
        @NotNull(message = "O ID do técnico é obrigatório")
        Integer technicianId,
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
