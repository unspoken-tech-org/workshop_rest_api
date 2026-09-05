package com.tproject.workshop.exception;

import java.io.Serial;

public class ApiKeyDeviceBoundException extends WorkshopException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ApiKeyDeviceBoundException(String message) {
        super(message);
    }
}
