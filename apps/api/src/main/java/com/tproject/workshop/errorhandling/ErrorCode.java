package com.tproject.workshop.errorhandling;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    ACCESS_DENIED("auth.access.denied", HttpStatus.FORBIDDEN, "Acesso Negado"),
    AUTHENTICATION_REQUIRED("authentication.required", HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_API_KEY("auth.invalid.api.key", HttpStatus.UNAUTHORIZED, "API Key Inválida"),
    INVALID_TOKEN("auth.invalid.token", HttpStatus.UNAUTHORIZED, "Token Inválido"),
    TOKEN_EXPIRED("auth.token.expired", HttpStatus.UNAUTHORIZED, "Token Expirado"),
    API_KEY_DEVICE_BOUND("auth.api_key.device.bound", HttpStatus.CONFLICT, "API Key Vinculada"),
    ENTITY_NOT_FOUND(
            "entity.not.found.for.request", HttpStatus.NOT_FOUND, "Entidade não encontrada"),
    BAD_REQUEST("requisicao.invalida", HttpStatus.BAD_REQUEST, "Requisição Inválida"),
    VALIDATION_ERROR("erro.validacao", HttpStatus.BAD_REQUEST, "Erro de Validação"),
    RESOURCE_CONFLICT("recurso.conflito", HttpStatus.CONFLICT, "Conflito de Recurso"),
    VERSION_CONFLICT("recurso.conflito.versao", HttpStatus.CONFLICT, "Conflito de Versão"),
    PAYLOAD_TOO_LARGE("payload.too.large", HttpStatus.PAYLOAD_TOO_LARGE, "Payload muito grande"),
    DATABASE_ERROR("database.error", HttpStatus.INTERNAL_SERVER_ERROR, "Erro de Banco de Dados"),
    INTERNAL_SERVER_ERROR(
            "internal.server.error", HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno");

    private final String code;
    private final HttpStatus status;
    private final String title;

    ErrorCode(String code, HttpStatus status, String title) {
        this.code = code;
        this.status = status;
        this.title = title;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
