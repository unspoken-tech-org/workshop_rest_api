package com.tproject.workshop.integration;

import org.apache.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.jdbc.Sql;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

/**
 * SpringDoc is off by default (application.yml) and only the local profile turns
 * it on, so the test profile mirrors QA and PRD: no public OpenAPI or Swagger UI.
 */
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql",
        "/test-scripts/resetTablesSequence.sql"})
class SpringDocDisabledIT extends AbstractIntegrationLiveTest {

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/v3/api-docs/swagger-config", "/swagger-ui.html",
            "/swagger-ui/index.html"})
    void shouldNotExposeSpringDocAnonymously(String path) {
        given().spec(SPEC)
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui.html"})
    void shouldNotServeSpringDocToAuthenticatedClients(String path) {
        given().spec(getAuthenticatedSpec())
                .when()
                .get(path)
                .then()
                .statusCode(not(HttpStatus.SC_OK));
    }
}
