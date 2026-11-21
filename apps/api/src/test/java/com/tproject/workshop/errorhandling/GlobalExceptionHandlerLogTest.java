package com.tproject.workshop.errorhandling;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import jakarta.persistence.OptimisticLockException;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;

class GlobalExceptionHandlerLogTest {

    private static final String SENTINEL_CPF = "317.814.770-51";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void shouldLogClientErrorByCodeAndRouteWithoutMessageValues() {
        handler.handleEntityAlreadyExistsException(
                new EntityAlreadyExistsException("O CPF " + SENTINEL_CPF + " já está em uso"),
                request());

        String logged = appender.list.get(0).getFormattedMessage();
        assertThat(logged).contains("route=/v1/customers/{id}", "recurso.conflito");
        assertThat(logged).doesNotContain(SENTINEL_CPF, "31781477051");
    }

    @Test
    void shouldLogDatabaseErrorWithoutMessageInLogLine() {
        handler.handleDatabaseException(
                new DataIntegrityViolationException(
                        "Key (cpf)=(" + SENTINEL_CPF + ") already exists"),
                request());

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getFormattedMessage())
                .contains("database.error")
                .doesNotContain(SENTINEL_CPF);
        assertThat(ThrowableProxyUtil.asString(event.getThrowableProxy())).isNotBlank();
    }

    @Test
    void shouldMapUniqueConstraintViolationTo409ConflictWithoutLeakingValues() {
        SQLException sqlEx =
                new SQLException("duplicate key value violates unique constraint", "23505");
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("could not execute statement", sqlEx);

        ResponseEntity<ResponseError> response =
                handler.handleDataIntegrityViolationException(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getErrors().getCode()).isEqualTo("recurso.conflito");
        assertThat(appender.list.get(0).getFormattedMessage()).contains("recurso.conflito");
    }

    @Test
    void shouldMapNonUniqueDataIntegrityViolationTo500DatabaseError() {
        SQLException sqlEx = new SQLException("null value in column", "23502");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("not null", sqlEx);

        ResponseEntity<ResponseError> response =
                handler.handleDataIntegrityViolationException(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrors().getCode()).isEqualTo("database.error");
    }

    @Test
    void shouldMapOptimisticLockingExceptionsTo409Conflict() {
        ResponseEntity<ResponseError> springResponse =
                handler.handleOptimisticLockingException(
                        new ObjectOptimisticLockingFailureException("devices", 1), request());
        ResponseEntity<ResponseError> jpaResponse =
                handler.handleOptimisticLockingException(
                        new OptimisticLockException("stale"), request());

        assertThat(springResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(springResponse.getBody()).isNotNull();
        assertThat(springResponse.getBody().getErrors().getCode())
                .isEqualTo("recurso.conflito.versao");

        assertThat(jpaResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(jpaResponse.getBody()).isNotNull();
        assertThat(jpaResponse.getBody().getErrors().getCode())
                .isEqualTo("recurso.conflito.versao");
    }

    private static ServletWebRequest request() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("PUT", "/v1/customers/31781477051");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/customers/{id}");
        return new ServletWebRequest(request);
    }
}
