package com.tproject.workshop.errorhandling;

import com.tproject.workshop.config.logging.RequestLogSanitizer;
import com.tproject.workshop.exception.ApiKeyDeviceBoundException;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.InvalidApiKeyException;
import com.tproject.workshop.exception.InvalidTokenException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.exception.TokenExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.ObjectError;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseError> handleAccessDeniedException(final AccessDeniedException ex, WebRequest request) {
        logClientError("auth.access.denied", ex, request);

        ErrorMetadata.Error error = new ErrorMetadata.Error("auth.access.denied", "Acesso Negado: Você não tem permissão para realizar esta operação.");

        return new ResponseEntity<>(new ResponseError(HttpStatus.FORBIDDEN.value(), "Acesso Negado",
                error), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({NotFoundException.class, EmptyResultDataAccessException.class})
    public ResponseEntity<ResponseError> handleNotFoundException(final Exception ex, WebRequest request) {
        logClientError("entity.not.found.for.request", ex, request);
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("entity.not.found.for.request", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.NOT_FOUND.value(), "Entidade não encontrada",
                error), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ResponseError> handleBadRequestException(final BadRequestException ex, WebRequest request) {
        logClientError("requisicao.invalida", ex, request);
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("requisicao.invalida", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.BAD_REQUEST.value(), "Requisição Inválida",
                error), HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause instanceof BadRequestException badRequestEx) {
            return buildBadRequestResponse(badRequestEx.getMessage());
        }

        // The parser message may echo payload values; log only the cause type.
        logClientError("requisicao.invalida", rootCause, request);

        return buildBadRequestResponse("Payload inválido ou malformado. Verifique o JSON enviado.");
    }

    private ResponseEntity<Object> buildBadRequestResponse(String message) {
        ErrorMetadata.Error error = new ErrorMetadata.Error("requisicao.invalida", message);
        return new ResponseEntity<>(
                new ResponseError(HttpStatus.BAD_REQUEST.value(), "Requisição Inválida", error),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityAlreadyExistsException.class)
    public ResponseEntity<ResponseError> handleEntityAlreadyExistsException(final EntityAlreadyExistsException ex, WebRequest request) {
        logClientError("recurso.conflito", ex, request);
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("recurso.conflito", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.CONFLICT.value(), "Conflito de Recurso",
                error), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ApiKeyDeviceBoundException.class)
    public ResponseEntity<ResponseError> handleApiKeyDeviceBoundException(final ApiKeyDeviceBoundException ex, WebRequest request) {
        logClientError("auth.api_key.device.bound", ex, request);

        ErrorMetadata.Error error = new ErrorMetadata.Error("auth.api_key.device.bound", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.CONFLICT.value(), "API Key Vinculada",
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
                RequestLogSanitizer.route(request), fieldErrors, globalErrors);

        ResponseError responseError = getResponseError(fieldErrors, globalErrors);

        return new ResponseEntity<>(responseError, headers, HttpStatus.BAD_REQUEST);
    }

    // Client errors are logged by stable code and exception type only:
    // exception messages may carry values supplied by the user.
    private static void logClientError(String code, Throwable ex, WebRequest request) {
        EXCEPTION_LOGGER.warn("Client error for request: {} | Code: {} | ExceptionType: {}",
                RequestLogSanitizer.route(request), code, ex.getClass().getSimpleName());
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

    @ExceptionHandler(InvalidApiKeyException.class)
    public ResponseEntity<ResponseError> handleInvalidApiKeyException(final InvalidApiKeyException ex, WebRequest request) {
        logClientError("auth.invalid.api.key", ex, request);
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("auth.invalid.api.key", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.UNAUTHORIZED.value(), "API Key Inválida",
                error), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ResponseError> handleInvalidTokenException(final InvalidTokenException ex, WebRequest request) {
        logClientError("auth.invalid.token", ex, request);
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("auth.invalid.token", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.UNAUTHORIZED.value(), "Token Inválido",
                error), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ResponseError> handleTokenExpiredException(final TokenExpiredException ex, WebRequest request) {
        logClientError("auth.token.expired", ex, request);
        
        ErrorMetadata.Error error = new ErrorMetadata.Error("auth.token.expired", ex.getMessage());

        return new ResponseEntity<>(new ResponseError(HttpStatus.UNAUTHORIZED.value(), "Token Expirado",
                error), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseError> handleGenericException(final Exception ex, WebRequest request) {
        // The stack trace is kept for diagnosis; messages are not repeated in
        // the log line. Unknown exception messages remain a residual risk.
        EXCEPTION_LOGGER.error(
                "Unhandled exception for request: {} | Code: internal.server.error | ExceptionType: {}",
                RequestLogSanitizer.route(request),
                ex.getClass().getName(),
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
        // PostgreSQL "Detail" (row values) is disabled by logServerErrorDetail=false.
        EXCEPTION_LOGGER.error(
                "Database error for request: {} | Code: database.error | ExceptionType: {}",
                RequestLogSanitizer.route(request),
                ex.getClass().getName(),
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
