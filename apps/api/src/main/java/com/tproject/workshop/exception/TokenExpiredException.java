package com.tproject.workshop.exception;

import java.io.Serial;

public class TokenExpiredException extends WorkshopException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Exception cause) {
        super(message, cause);
    }
}
