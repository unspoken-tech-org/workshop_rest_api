package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class EntityAlreadyExistsException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;

    public EntityAlreadyExistsException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }
}
