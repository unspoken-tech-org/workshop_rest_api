package com.tproject.workshop.errorhandling;

import com.tproject.workshop.config.logging.RequestLogSanitizer;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.DomainException;
import com.tproject.workshop.exception.PayloadTooLargeException;
import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Component
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger EXCEPTION_LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public static final String PAYLOAD_TOO_LARGE_CODE = ErrorCode.PAYLOAD_TOO_LARGE.code();

    public static final String ACCESS_DENIED_MESSAGE =
            "Acesso Negado: Você não tem permissão para realizar esta operação.";

    public static final String AUTHENTICATION_REQUIRED_MESSAGE =
            "Autenticação necessária para acessar este recurso.";

    private static final String INTERNAL_ERROR_MESSAGE =
            "Ocorreu um erro interno. Por favor, tente novamente ou contate o suporte.";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(
            final DomainException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode.status().is5xxServerError()) {
            EXCEPTION_LOGGER.error(
                    "Domain server error for request: {} | Code: {} | ExceptionType: {}",
                    RequestLogSanitizer.route(request),
                    errorCode.code(),
                    ex.getClass().getName(),
                    ex);
            return buildResponse(errorCode, INTERNAL_ERROR_MESSAGE);
        }

        logClientError(errorCode.code(), ex, request);
        return buildResponse(errorCode, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            final AccessDeniedException ex, WebRequest request) {
        logClientError(ErrorCode.ACCESS_DENIED.code(), ex, request);
        return buildResponse(ErrorCode.ACCESS_DENIED, ACCESS_DENIED_MESSAGE);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ProblemDetail> handleEmptyResultDataAccessException(
            final EmptyResultDataAccessException ex, WebRequest request) {
        logClientError(ErrorCode.ENTITY_NOT_FOUND.code(), ex, request);
        return buildResponse(ErrorCode.ENTITY_NOT_FOUND, ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // A body over the limit fails while Jackson reads the stream and arrives wrapped here;
        // it must stay 413, not become 400.
        PayloadTooLargeException tooLarge = findCause(ex, PayloadTooLargeException.class);
        if (tooLarge != null) {
            logClientError(PAYLOAD_TOO_LARGE_CODE, tooLarge, request);
            return new ResponseEntity<>(
                    payloadTooLarge(tooLarge), ErrorCode.PAYLOAD_TOO_LARGE.status());
        }

        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause instanceof BadRequestException badRequestEx) {
            return buildBadRequestResponse(badRequestEx.getMessage());
        }

        // The parser message may echo payload values; log only the cause type.
        logClientError(ErrorCode.BAD_REQUEST.code(), rootCause, request);

        return buildBadRequestResponse("Payload inválido ou malformado. Verifique o JSON enviado.");
    }

    @ExceptionHandler(PayloadTooLargeException.class)
    public ResponseEntity<ProblemDetail> handlePayloadTooLargeException(
            final PayloadTooLargeException ex, WebRequest request) {
        logClientError(PAYLOAD_TOO_LARGE_CODE, ex, request);
        return new ResponseEntity<>(payloadTooLarge(ex), ErrorCode.PAYLOAD_TOO_LARGE.status());
    }

    /** 413 body, shared with {@code RequestBodySizeLimitFilter} for the Content-Length path. */
    public static ProblemDetail payloadTooLarge(PayloadTooLargeException ex) {
        return toProblemDetail(ErrorCode.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    public static ProblemDetail toProblemDetail(ErrorCode errorCode, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), message);
        problem.setTitle(errorCode.title());
        problem.setProperty("code", errorCode.code());
        return problem;
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private ResponseEntity<Object> buildBadRequestResponse(String message) {
        return new ResponseEntity<>(
                toProblemDetail(ErrorCode.BAD_REQUEST, message), ErrorCode.BAD_REQUEST.status());
    }

    @ExceptionHandler({
        ObjectOptimisticLockingFailureException.class,
        OptimisticLockException.class
    })
    public ResponseEntity<ProblemDetail> handleOptimisticLockingException(
            final Exception ex, WebRequest request) {
        logClientError(ErrorCode.VERSION_CONFLICT.code(), ex, request);
        return buildResponse(
                ErrorCode.VERSION_CONFLICT,
                "O registro foi atualizado por outro usuário. Recarregue os dados e tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
            final DataIntegrityViolationException ex, WebRequest request) {
        if (isUniqueConstraintViolation(ex)) {
            logClientError(ErrorCode.RESOURCE_CONFLICT.code(), ex, request);
            return buildResponse(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Registro duplicado: o recurso informado já existe.");
        }

        return handleDatabaseException(ex, request);
    }

    private static boolean isUniqueConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlEx && "23505".equals(sqlEx.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // The log names each field; the response carries only the messages,
        // which are written for the user and shown as they are by the front.
        String fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        String fieldMessages =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining("; "));

        String globalErrors =
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(ObjectError::getDefaultMessage)
                        .collect(Collectors.joining("; "));

        EXCEPTION_LOGGER.warn(
                "Validation error for request: {} | Fields: {} | Global: {}",
                RequestLogSanitizer.route(request),
                fieldErrors,
                globalErrors);

        ProblemDetail problem = buildValidationProblemDetail(fieldMessages, globalErrors);

        return new ResponseEntity<>(problem, headers, ErrorCode.VALIDATION_ERROR.status());
    }

    // Client errors are logged by stable code and exception type only:
    // exception messages may carry values supplied by the user.
    private static void logClientError(String code, Throwable ex, WebRequest request) {
        EXCEPTION_LOGGER.warn(
                "Client error for request: {} | Code: {} | ExceptionType: {}",
                RequestLogSanitizer.route(request),
                code,
                ex.getClass().getSimpleName());
    }

    private static ProblemDetail buildValidationProblemDetail(
            String fieldErrors, String globalErrors) {
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

        return toProblemDetail(ErrorCode.VALIDATION_ERROR, errorMessage.toString());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            final Exception ex, WebRequest request) {
        // The stack trace is kept for diagnosis; messages are not repeated in
        // the log line. Unknown exception messages remain a residual risk.
        EXCEPTION_LOGGER.error(
                "Unhandled exception for request: {} | Code: {} | ExceptionType: {}",
                RequestLogSanitizer.route(request),
                ErrorCode.INTERNAL_SERVER_ERROR.code(),
                ex.getClass().getName(),
                ex);

        return buildResponse(ErrorCode.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE);
    }

    @ExceptionHandler({DataAccessException.class, SQLException.class})
    public ResponseEntity<ProblemDetail> handleDatabaseException(
            final Exception ex, WebRequest request) {
        // PostgreSQL "Detail" (row values) is disabled by logServerErrorDetail=false.
        EXCEPTION_LOGGER.error(
                "Database error for request: {} | Code: {} | ExceptionType: {}",
                RequestLogSanitizer.route(request),
                ErrorCode.DATABASE_ERROR.code(),
                ex.getClass().getName(),
                ex);

        return buildResponse(
                ErrorCode.DATABASE_ERROR,
                "Erro ao acessar o banco de dados. Por favor, tente novamente.");
    }

    private static ResponseEntity<ProblemDetail> buildResponse(
            ErrorCode errorCode, String message) {
        return new ResponseEntity<>(toProblemDetail(errorCode, message), errorCode.status());
    }
}
