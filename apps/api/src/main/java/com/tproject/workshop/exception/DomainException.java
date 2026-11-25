package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;
import java.util.Objects;

public abstract class DomainException extends RuntimeException {

    @Serial private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    protected DomainException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
