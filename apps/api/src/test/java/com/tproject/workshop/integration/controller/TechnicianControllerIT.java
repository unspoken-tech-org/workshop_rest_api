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

class TechnicianControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/technician";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;

    @BeforeEach
    void seed() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
    }

    @Test
    @DisplayName("Rejects access without JWT")
    void shouldReturn401_whenNoJwt() {
        given().spec(SPEC).get(BASE_PATH).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Lists technicians")
    void shouldListTechnicians_whenAuthenticated() {
        int first = fixtures.technician("Technician 1", "44988254493");
        int second = fixtures.technician("Technician 2", "44988254494");

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getList("id", Integer.class)).containsExactlyInAnyOrder(first, second);
        assertThat(body.getList("name", String.class))
                .containsExactlyInAnyOrder("Technician 1", "Technician 2");
        assertThat(body.getList("number", String.class))
                .containsExactlyInAnyOrder("44988254493", "44988254494");
    }

    @Test
    @DisplayName("Returns an empty list when there are no technicians")
    void shouldReturnEmptyList_whenThereAreNoTechnicians() {
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
    @DisplayName("Finds a technician by id")
    void shouldFindTechnician_whenIdExists() {
        int id = fixtures.technician("Technician 1", "44988254493");

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH + "/" + id)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getInt("id")).isEqualTo(id);
        assertThat(body.getString("name")).isEqualTo("Technician 1");
        assertThat(body.getString("number")).isEqualTo("44988254493");
    }

    @Test
    @DisplayName("Returns 404 for an unknown technician")
    void shouldReturn404_whenTechnicianDoesNotExist() {
        int unknown = fixtures.technician("Technician 1", "44988254493") + 999;

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH + "/" + unknown)
                        .then()
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getString("code")).isEqualTo("entity.not.found.for.request");
        assertThat(body.getString("detail"))
                .isEqualTo("Técnico com id " + unknown + " não encontrado");
    }
}
