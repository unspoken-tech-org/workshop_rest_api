package com.tproject.workshop.exception;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class WorkshopException  extends RuntimeException {

    private static final long serialVersionUID = -8592554845261561969L;
    private final List<String> errors;

    public WorkshopException(List<String> errors) {
        super("Validation Exception");
        this.errors = Objects.requireNonNullElse(errors, new ArrayList<>());
    }

    public WorkshopException(String error) {
        super(error);
        this.errors = Collections.emptyList();
    }

    public WorkshopException(Exception exception) {
        super(exception);
        this.errors = Collections.emptyList();
    }

    public WorkshopException(String error, Exception exception) {
        super(error, exception);
        this.errors = Collections.emptyList();
    }

}
