package com.tproject.workshop.dto.contact;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tproject.workshop.config.jackson.PhoneNumberDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerContactInputDto(
        @Schema(description = "Device identifier associated with the customer contact", example = "42")
        @NotNull(message = "O ID do dispositivo é obrigatório")
        Integer deviceId,
        @Schema(description = "Channel used to reach the customer", example = "WHATSAPP")
        String contactType,
        @Schema(description = "Technician responsible for the contact", example = "7")
        @NotNull(message = "O ID do técnico é obrigatório")
        Integer technicianId,
        @Schema(description = "Optional phone number used during the contact", example = "11988887777")
        @Size(min = 10, max = 11, message = "O número de telefone, se preenchido, deve ter no mínimo 10 caracteres e no máximo 11")
        @JsonDeserialize(using = PhoneNumberDeserializer.class)
        String phoneNumber,
        @Schema(description = "Message sent to the customer", example = "Device ready for pickup, awaiting confirmation.")
        @NotEmpty(message = "A mensagem é obrigatória")
        String message,
        @Schema(description = "Flag indicating if the customer replied or acknowledged the contact", example = "true")
        @NotNull(message = "O status do contato é obrigatório")
        Boolean contactStatus,
        @Schema(description = "Current device status when the contact was logged", example = "EM_ANDAMENTO")
        @NotNull(message = "O status do dispositivo é obrigatório")
        String deviceStatus,
        @Schema(description = "Date and time when the contact happened", example = "2025-08-31T19:55:13Z")
        @NotNull(message = "A data do contato é obrigatória")
        String contactDate
) {
}
