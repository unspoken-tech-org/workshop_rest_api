package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class TypesBrandsModelsControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/types-brands-models";

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
    @DisplayName("Returns an empty list when the catalog is empty")
    void shouldReturnEmptyList_whenCatalogIsEmpty() {
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
    @DisplayName("Nests brands and models under each type")
    void shouldReturnTypesWithBrandsAndModels_whenCatalogIsLinked() {
        int washer = fixtures.type("Lavadora");
        int microwave = fixtures.type("Micro-ondas");
        int samsung = fixtures.brand("Samsung");
        int lg = fixtures.brand("LG");
        int wt1 = fixtures.model("WT12345");
        int wt2 = fixtures.model("WT12300");
        int nn = fixtures.model("NN-ST25");
        fixtures.brandModelType(samsung, wt1, washer);
        fixtures.brandModelType(samsung, wt2, washer);
        fixtures.brandModelType(lg, wt1, washer);
        fixtures.brandModelType(lg, nn, microwave);
        // A type without any link must not appear in the mapping.
        fixtures.type("Freezer");

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getList("idType", Integer.class))
                .containsExactlyInAnyOrder(washer, microwave);
        Map<String, Object> washerNode = type(body, washer);
        assertThat(washerNode.get("type")).isEqualTo("lavadora");
        assertThat(brandIds(washerNode)).containsExactlyInAnyOrder(samsung, lg);
        assertThat(modelIds(brand(washerNode, samsung))).containsExactlyInAnyOrder(wt1, wt2);
        assertThat(modelIds(brand(washerNode, lg))).containsExactly(wt1);
        Map<String, Object> microwaveNode = type(body, microwave);
        assertThat(brandIds(microwaveNode)).containsExactly(lg);
        assertThat(modelIds(brand(microwaveNode, lg))).containsExactly(nn);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> type(JsonPath body, int id) {
        return body.getList("$", Map.class).stream()
                .map(node -> (Map<String, Object>) node)
                .filter(node -> ((Number) node.get("idType")).intValue() == id)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> brands(Map<String, Object> type) {
        return (List<Map<String, Object>>) type.get("brands");
    }

    private static List<Integer> brandIds(Map<String, Object> type) {
        return brands(type).stream().map(b -> ((Number) b.get("idBrand")).intValue()).toList();
    }

    private static Map<String, Object> brand(Map<String, Object> type, int id) {
        return brands(type).stream()
                .filter(b -> ((Number) b.get("idBrand")).intValue() == id)
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> modelIds(Map<String, Object> brand) {
        return ((List<Map<String, Object>>) brand.get("models"))
                .stream().map(m -> ((Number) m.get("idModel")).intValue()).toList();
    }
}
