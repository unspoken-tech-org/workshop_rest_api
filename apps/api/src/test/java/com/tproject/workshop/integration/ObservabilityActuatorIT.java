package com.tproject.workshop.integration;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/** Contract tests for the exposed Actuator surface and its authorization. */
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql",
        "/test-scripts/resetTablesSequence.sql"})
class ObservabilityActuatorIT extends AbstractIntegrationLiveTest {

    private static final String SCRAPE_USER = "metrics-scraper";
    // Matches the bcrypt hash configured only in application-test.yml.
    private static final String SCRAPE_PASSWORD = "test-only-metrics-scrape-password";

    @Test
    void shouldKeepOnlyHealthPublic() {
        given().spec(SPEC)
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("\"status\""));

        given().spec(SPEC)
                .when()
                .get("/actuator/info")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        // An authenticated role without permission gets 403, not 401 (R19).
        given().spec(getServiceAuthenticatedSpec())
                .when()
                .get("/actuator/info")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(containsString("auth.access.denied"));
    }

    @Test
    void shouldAllowPrometheusOnlyForScrapeIdentity() {
        given().spec(SPEC)
                .auth().preemptive().basic(SCRAPE_USER, SCRAPE_PASSWORD)
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .body(containsString("# HELP"))
                .body(containsString("http_server_requests_seconds_bucket"));
    }

    @Test
    void shouldDenyPrometheusWithoutValidScrapeCredential() {
        given().spec(SPEC)
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        given().spec(SPEC)
                .auth().preemptive().basic(SCRAPE_USER, "wrong-password")
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);

        // An admin session is not a scrape identity (least privilege).
        given().spec(getAuthenticatedSpec())
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
