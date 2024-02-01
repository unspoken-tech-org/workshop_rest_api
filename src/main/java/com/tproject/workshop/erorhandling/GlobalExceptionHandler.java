package com.tproject.workshop.erorhandling;

import com.tproject.workshop.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Component
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger EXCEPTION_LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NotFoundException.class, EmptyResultDataAccessException.class})
    public ResponseEntity<ResponseError> handleNotFoundException(final Exception ex, WebRequest request) {
        if (EXCEPTION_LOGGER.isWarnEnabled()) {
            EXCEPTION_LOGGER.warn("Entity not found for request: " + request.getDescription(false), ex);
        }

        return new ResponseEntity<>(new ResponseError(HttpStatus.NOT_FOUND.value(), "Entity not found for request",
                ErrorMetadata.builder().addError("entity.not.found.for.request",
                        ex.getMessage()).build()), HttpStatus.NOT_FOUND);
    }
}
