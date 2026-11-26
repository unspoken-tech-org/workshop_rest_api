package com.tproject.workshop.integration.security;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/** Requests rejected by Spring Security also produce an access log (R18). */
@Sql({"/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
class SecurityAccessLogIT extends AbstractIntegrationTest {

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger accessLogger;
    private Level previousLevel;

    @BeforeEach
    void attachAppender() {
        accessLogger = (Logger) LoggerFactory.getLogger("http.access");
        previousLevel = accessLogger.getLevel();
        accessLogger.setLevel(Level.INFO);
        appender.start();
        accessLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        accessLogger.detachAppender(appender);
        accessLogger.setLevel(previousLevel);
        appender.stop();
    }

    @Test
    void shouldLogUnauthenticatedRequestWithoutConcreteUri() {
        given().spec(SPEC)
                .when()
                .get("/v1/customer/31781477051")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        assertThat(messages())
                .singleElement()
                .satisfies(message -> {
                    assertThat(message).contains("GET", "unmatched", "401");
                    assertThat(message).doesNotContain("31781477051");
                });
    }

    @Test
    void shouldLogForbiddenAndDeniedActuatorRequests() {
        // Obtaining the token is itself a logged request.
        var serviceSpec = getServiceAuthenticatedSpec();
        appender.list.clear();

        given().spec(serviceSpec)
                .when()
                .get("/actuator/info")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        given().spec(SPEC)
                .auth()
                .preemptive()
                .basic("metrics-scraper", "wrong-password")
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        assertThat(messages()).hasSize(2);
        assertThat(messages().get(0)).contains("GET", "unmatched", "403");
        assertThat(messages().get(1)).contains("GET", "unmatched", "401");
    }

    @Test
    void shouldNotLogSuccessfulHealthChecks() {
        given().spec(SPEC).when().get("/actuator/health").then().statusCode(HttpStatus.OK.value());

        assertThat(messages()).isEmpty();
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
