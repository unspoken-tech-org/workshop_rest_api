package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class ColorsControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/colors";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;

    @BeforeEach
    void seed() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        fixtures.serviceApiKey();
    }

    @Test
    @DisplayName("Rejects access without JWT")
    void shouldReturn401_whenNoJwt() {
        given().spec(SPEC).get(BASE_PATH).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Lists every color with its id")
    void shouldListColors_whenAuthenticated() {
        int black = fixtures.color("preto");
        int white = fixtures.color("branco");

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getList("idColor", Integer.class)).containsExactlyInAnyOrder(black, white);
        assertThat(body.getList("color", String.class))
                .containsExactlyInAnyOrder("preto", "branco");
    }

    @Test
    @DisplayName("Returns an empty list when there are no colors")
    void shouldReturnEmptyList_whenThereAreNoColors() {
        List<Object> body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getList("$");

        assertThat(body).isEmpty();
    }

    @Test
    @DisplayName("SERVICE role can list colors")
    void shouldListColors_whenAuthenticatedAsService() {
        fixtures.color("azul");

        given().spec(getServiceAuthenticatedSpec())
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value());
    }
}
