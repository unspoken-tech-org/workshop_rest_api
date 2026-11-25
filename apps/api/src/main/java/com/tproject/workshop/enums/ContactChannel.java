package com.tproject.workshop.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.utils.UtilsString;
import java.util.Optional;

public enum ContactChannel {
    LIGACAO("ligacao", true, "ligacao"),
    MENSAGEM("mensagem", true, "mensagem"),
    PESSOALMENTE("pessoalmente", false, "other"),
    WHATSAPP("whatsapp", false, "other");

    private final String wireValue;
    private final boolean requiresPhone;
    private final String observabilityValue;

    ContactChannel(String wireValue, boolean requiresPhone, String observabilityValue) {
        this.wireValue = wireValue;
        this.requiresPhone = requiresPhone;
        this.observabilityValue = observabilityValue;
    }

    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    public boolean requiresPhone() {
        return requiresPhone;
    }

    public String observabilityValue() {
        return observabilityValue;
    }

    public static Optional<ContactChannel> fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(fromString(value));
    }

    @JsonCreator
    public static ContactChannel fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("O tipo de contato é obrigatório");
        }
        String normalized = UtilsString.normalizeString(value.trim());
        for (ContactChannel channel : values()) {
            if (channel.wireValue.equals(normalized)) {
                return channel;
            }
        }
        throw new BadRequestException(
                "Tipo de contato \"%s\" não é válido. Valores aceitos: ligacao, mensagem, pessoalmente, whatsapp",
                value);
    }
}
