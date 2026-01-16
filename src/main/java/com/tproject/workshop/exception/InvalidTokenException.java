package com.tproject.workshop.exception;

public class InvalidTokenException extends WorkshopException {

    private static final long serialVersionUID = 1L;

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Exception cause) {
        super(message, cause);
    }
}
