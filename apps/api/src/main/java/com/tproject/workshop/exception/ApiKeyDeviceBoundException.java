package com.tproject.workshop.exception;

import com.tproject.workshop.errorhandling.ErrorCode;
import java.io.Serial;

public class ApiKeyDeviceBoundException extends WorkshopException {

    @Serial private static final long serialVersionUID = 1L;

    public ApiKeyDeviceBoundException(String message) {
        super(ErrorCode.API_KEY_DEVICE_BOUND, message);
    }
}
