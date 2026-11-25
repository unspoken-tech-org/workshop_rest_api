package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class InvalidTokenException extends WorkshopException {

    @Serial private static final long serialVersionUID = 1L;

    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }

    public InvalidTokenException(String message, Exception cause) {
        super(ErrorCode.INVALID_TOKEN, message, cause);
    }
}
