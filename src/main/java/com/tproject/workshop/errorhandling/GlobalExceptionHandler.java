package com.tproject.workshop.errorhandling;

import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
@Component
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger EXCEPTION_LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NotFoundException.class, EmptyResultDataAccessException.class})
    public ResponseEntity<ResponseError> handleNotFoundException(final Exception ex, WebRequest request) {
        if (EXCEPTION_LOGGER.isWarnEnabled()) {
            EXCEPTION_LOGGER.warn("Entity not found for request: {}", request.getDescription(false), ex);
        }
        ErrorMetadata.Error error = new ErrorMetadata.Error("entity.not.found.for.request", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.NOT_FOUND.value(), "Entitdade não encontrada",
                error), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseError> handleBadRequestException(final BadRequestException ex, WebRequest request) {
        if (EXCEPTION_LOGGER.isWarnEnabled()) {
            EXCEPTION_LOGGER.warn("Bad request: {}", request.getDescription(false), ex);
        }
        ErrorMetadata.Error error = new ErrorMetadata.Error("requisicao.invalida", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Requisição Inválida",
                error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ResponseError> handleEntityAlreadyExistsException(final EntityAlreadyExistsException ex, WebRequest request) {
        if (EXCEPTION_LOGGER.isWarnEnabled()) {
            EXCEPTION_LOGGER.warn("Conflict detected for request: {}", request.getDescription(false), ex);
        }
        ErrorMetadata.Error error = new ErrorMetadata.Error("recurso.conflito", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.CONFLICT.value(), "Conflito de Recurso",
                error), HttpStatus.CONFLICT);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        if (EXCEPTION_LOGGER.isWarnEnabled()) {
            EXCEPTION_LOGGER.warn("Validation error for request: {}", request.getDescription(false), ex);
        }

        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        String globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ResponseError responseError = getResponseError(fieldErrors, globalErrors);

        return new ResponseEntity<>(responseError, headers, HttpStatus.BAD_REQUEST);
    }

    private static ResponseError getResponseError(String fieldErrors, String globalErrors) {
        StringBuilder errorMessage = new StringBuilder();
        if (!fieldErrors.isEmpty()) {
            errorMessage.append(fieldErrors);
        }
        if (!globalErrors.isEmpty()) {
            if (!errorMessage.isEmpty()) {
                errorMessage.append("; ");
            }
            errorMessage.append(globalErrors);
        }

        if (errorMessage.isEmpty()) {
            errorMessage.append("Erro de validação desconhecido.");
        }

        ErrorMetadata.Error error = new ErrorMetadata.Error("erro.validacao", errorMessage.toString());
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), "Erro de Validação", error);
    }
}
