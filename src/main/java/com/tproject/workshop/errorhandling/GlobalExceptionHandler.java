package com.tproject.workshop.errorhandling;

import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

import java.sql.SQLException;
import java.util.stream.Collectors;

@ControllerAdvice
@Component
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger EXCEPTION_LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NotFoundException.class, EmptyResultDataAccessException.class})
    public ResponseEntity<ResponseError> handleNotFoundException(final Exception ex, WebRequest request) {
        EXCEPTION_LOGGER.warn("Entity not found for request: {} | Message: {}",
                request.getDescription(false), ex.getMessage());
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("entity.not.found.for.request", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.NOT_FOUND.value(), "Entidade não encontrada",
                error), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseError> handleBadRequestException(final BadRequestException ex, WebRequest request) {
        EXCEPTION_LOGGER.warn("Bad request: {} | Message: {}",
                request.getDescription(false), ex.getMessage());
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("requisicao.invalida", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Requisição Inválida",
                error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ResponseError> handleEntityAlreadyExistsException(final EntityAlreadyExistsException ex, WebRequest request) {
        EXCEPTION_LOGGER.warn("Conflict detected for request: {} | Message: {}",
                request.getDescription(false), ex.getMessage());
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("recurso.conflito", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.CONFLICT.value(), "Conflito de Recurso",
                error), HttpStatus.CONFLICT);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        String globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        EXCEPTION_LOGGER.warn("Validation error for request: {} | Fields: {} | Global: {}",
                request.getDescription(false), fieldErrors, globalErrors);

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseError> handleGenericException(final Exception ex, WebRequest request) {
        EXCEPTION_LOGGER.error(
                "Unhandled exception for request: {} | ExceptionType: {} | Message: {}",
                request.getDescription(false),
                ex.getClass().getName(),
                ex.getMessage(),
                ex  
        );

        ErrorMetadata.Error error = new ErrorMetadata.Error(
                "internal.server.error",
                "Ocorreu um erro interno. Por favor, tente novamente ou contate o suporte."
        );

        return new ResponseEntity<>(
                new ResponseError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro Interno", error),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ResponseError> handleDatabaseException(final Exception ex, WebRequest request) {
        EXCEPTION_LOGGER.error(
                "Database error for request: {} | Type: {} | Message: {}",
                request.getDescription(false),
                ex.getClass().getName(),
                ex.getMessage(),
                ex
        );

        ErrorMetadata.Error error = new ErrorMetadata.Error(
                "database.error",
                "Erro ao acessar o banco de dados. Por favor, tente novamente."
        );

        return new ResponseEntity<>(
                new ResponseError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro de Banco de Dados", error),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
