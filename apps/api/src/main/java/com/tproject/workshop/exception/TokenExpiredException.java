package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class TokenExpiredException extends WorkshopException {

    @Serial private static final long serialVersionUID = 1L;

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }

    public TokenExpiredException(String message, Exception cause) {
        super(ErrorCode.TOKEN_EXPIRED, message, cause);
    }
}
