package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

/** Same {@code Idempotency-Key} sent with a different request body (422). */
public class IdempotencyKeyReusedException extends DomainException {

    @Serial private static final long serialVersionUID = 1L;

    public IdempotencyKeyReusedException() {
        super(
                ErrorCode.IDEMPOTENCY_KEY_REUSED,
                "A Idempotency-Key já foi usada em outra requisição com conteúdo diferente.");
    }
}
