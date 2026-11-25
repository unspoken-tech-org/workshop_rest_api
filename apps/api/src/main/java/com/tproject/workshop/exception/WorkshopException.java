package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;

@Getter
public class WorkshopException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;
    private final List<String> errors;

    public WorkshopException(List<String> errors) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, "Validation Exception");
        this.errors = Objects.requireNonNullElse(errors, new ArrayList<>());
    }

    public WorkshopException(String error) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, error);
        this.errors = Collections.emptyList();
    }

    public WorkshopException(Exception exception) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        this.errors = Collections.emptyList();
    }

    public WorkshopException(String error, Exception exception) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, error, exception);
        this.errors = Collections.emptyList();
    }

    protected WorkshopException(ErrorCode errorCode, String error) {
        super(errorCode, error);
        this.errors = Collections.emptyList();
    }

    protected WorkshopException(ErrorCode errorCode, String error, Throwable cause) {
        super(errorCode, error, cause);
        this.errors = Collections.emptyList();
    }
}
