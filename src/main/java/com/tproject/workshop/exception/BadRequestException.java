package com.tproject.workshop.exception;

import java.io.Serial;

public class BadRequestException extends RuntimeException{

        @Serial
        private static final long serialVersionUID = -8483484780662701254L;

        public BadRequestException() {
            super();
        }

        public BadRequestException(String message) {
            super(message);
        }

        public BadRequestException(String message, Object... messageParams) {
            super(String.format(message, messageParams));
        }

        public BadRequestException(String message, Throwable throwable) {
            super(message, throwable);
        }
}
