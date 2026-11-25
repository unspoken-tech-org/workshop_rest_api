package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class InvalidApiKeyException extends WorkshopException {

    @Serial private static final long serialVersionUID = 1L;

    public InvalidApiKeyException(String message) {
        super(ErrorCode.INVALID_API_KEY, message);
    }

    public InvalidApiKeyException(String message, Exception cause) {
        super(ErrorCode.INVALID_API_KEY, message, cause);
    }
}
