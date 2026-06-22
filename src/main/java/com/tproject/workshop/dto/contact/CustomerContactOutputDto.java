package com.tproject.workshop.dto.contact;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Customer contact details response")
public record CustomerContactOutputDto(
        @Schema(example = "1")
        Integer id,

        @Schema(example = "10")
        int deviceId,

        @Schema(example = "5")
        int technicianId,

        @Schema(example = "John Technician")
        String technicianName,

        @Schema(example = "(11) 99999-9999")
        String phone,

        @Schema(example = "WHATSAPP")
        String type,

        @Schema(example = "true")
        boolean hasMadeContact,

        @Schema(hidden = true)
        LocalDateTime lastContact,

        @Schema(example = "Aparelho pronto para retirada")
        String conversation,

        @Schema(example = "NOVO, PRONTO, APROVADO")
        String deviceStatus
) {
}
