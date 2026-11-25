package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class NotFoundException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(ErrorCode.ENTITY_NOT_FOUND, message);
    }

    public NotFoundException(String message, Exception exception) {
        super(ErrorCode.ENTITY_NOT_FOUND, message, exception);
    }

    public NotFoundException(String message, Object... messageParams) {
        super(ErrorCode.ENTITY_NOT_FOUND, String.format(message, messageParams));
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException("%s com id %s não encontrado", entity, id);
    }

    public static NotFoundException ofFeminine(String entity, Object id) {
        return new NotFoundException("%s com id %s não encontrada", entity, id);
    }
}
