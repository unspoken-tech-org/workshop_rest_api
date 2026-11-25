package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class BadRequestException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;

    public BadRequestException() {
        super(ErrorCode.BAD_REQUEST, (String) null);
    }

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Object... messageParams) {
        super(ErrorCode.BAD_REQUEST, String.format(message, messageParams));
    }

    public BadRequestException(String message, Throwable throwable) {
        super(ErrorCode.BAD_REQUEST, message, throwable);
    }
}
