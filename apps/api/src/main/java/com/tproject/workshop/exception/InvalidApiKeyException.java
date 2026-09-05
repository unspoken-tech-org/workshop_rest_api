package com.tproject.workshop.exception;

import java.io.Serial;

public class InvalidApiKeyException extends WorkshopException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidApiKeyException(String message) {
        super(message);
    }

    public InvalidApiKeyException(String message, Exception cause) {
        super(message, cause);
    }
}
