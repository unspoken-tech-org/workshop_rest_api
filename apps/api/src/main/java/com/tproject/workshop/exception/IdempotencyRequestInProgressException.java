package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

/** A request with the same {@code Idempotency-Key} is still being processed (409). */
public class IdempotencyRequestInProgressException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;

    public IdempotencyRequestInProgressException() {
        super(
                ErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
                "Já existe uma requisição em processamento com esta Idempotency-Key.");
    }
}
