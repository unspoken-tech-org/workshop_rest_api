package com.tproject.workshop.integration.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * SpringDoc is off by default (application.yml) and only the local profile turns it on, so the test
 * profile mirrors QA and PRD: no public OpenAPI or Swagger UI.
 */
@Sql({"/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
class SpringDocDisabledIT extends AbstractIntegrationTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/v3/api-docs",
                "/v3/api-docs/swagger-config",
                "/swagger-ui.html",
                "/swagger-ui/index.html"
            })
    void shouldNotExposeSpringDocAnonymously(String path) {
        given().spec(SPEC).when().get(path).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/swagger-ui.html"})
    void shouldNotServeSpringDocToAuthenticatedClients(String path) {
        given().spec(getAuthenticatedSpec())
                .when()
                .get(path)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("authentication.required"));
    }
}
