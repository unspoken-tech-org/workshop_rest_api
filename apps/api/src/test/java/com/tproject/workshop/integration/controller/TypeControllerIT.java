package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class TypeControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/type";

    @Autowired private ApplicationContext context;

    private int lavadoraId;

    @BeforeEach
    void seedCatalog() {
        IntegrationFixtures fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        lavadoraId = fixtures.type("Lavadora");
        for (String type :
                List.of(
                        "Lava e Seca",
                        "Lavadora Industrial",
                        "Micro-ondas",
                        "Refrigerador",
                        "Freezer",
                        "Fogão",
                        "Air Fryer",
                        "Aspirador",
                        "Chuveiro Elétrico")) {
            fixtures.type(type);
        }
    }

    @Test
    @DisplayName("Rejects search without JWT")
    void shouldReturn401_whenSearchingWithoutJwt() {
        given().spec(SPEC)
                .body(Map.of())
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Empty query lists every type")
    void shouldListAllTypes_whenQueryIsEmpty() {
        JsonPath body = search(Map.of());

        assertThat(body.getList("content.type", String.class))
                .containsExactlyInAnyOrder(
                        "lavadora",
                        "lava e seca",
                        "lavadora industrial",
                        "micro-ondas",
                        "refrigerador",
                        "freezer",
                        "fogão",
                        "air fryer",
                        "aspirador",
                        "chuveiro elétrico");
        assertThat(body.getInt("totalElements")).isEqualTo(10);
        assertThat(body.getInt("size")).isEqualTo(15);
    }

    @ParameterizedTest(name = "query \"{0}\" -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "lav|lavadora,lava e seca,lavadora industrial",
                "LAV|lavadora,lava e seca,lavadora industrial",
                "la|lavadora,lava e seca,lavadora industrial",
                "micro|micro-ondas",
                "refrigerador|refrigerador",
                "e seca|lava e seca",
                "industrial|lavadora industrial",
                "xyz123|"
            })
    @DisplayName("Filters types by query")
    void shouldFilterTypes_whenQueryIsGiven(String query, String expected) {
        JsonPath body = search(Map.of("query", query));

        List<String> expectedTypes = expected == null ? List.of() : List.of(expected.split(","));
        assertThat(body.getList("content.type", String.class))
                .containsExactlyInAnyOrderElementsOf(expectedTypes);
        assertThat(body.getInt("totalElements")).isEqualTo(expectedTypes.size());
    }

    @Test
    @DisplayName("Search returns the persisted id of each type")
    void shouldReturnTypeId_whenSearching() {
        JsonPath body = search(Map.of("query", "lavadora industrial"));

        Integer expectedId =
                jdbcTemplate.queryForObject(
                        "SELECT id FROM types WHERE type = 'lavadora industrial'", Integer.class);
        assertThat(body.getList("content.idType", Integer.class)).containsExactly(expectedId);
    }

    @Test
    @DisplayName("Paginates results")
    void shouldPaginate_whenPageAndSizeAreGiven() {
        JsonPath first = search(Map.of("page", 0, "size", 2));
        JsonPath second = search(Map.of("page", 1, "size", 2));

        assertThat(first.getList("content")).hasSize(2);
        assertThat(first.getInt("totalElements")).isEqualTo(10);
        assertThat(first.getInt("totalPages")).isEqualTo(5);
        assertThat(first.getBoolean("last")).isFalse();
        assertThat(second.getInt("number")).isEqualTo(1);
        assertThat(second.getList("content")).hasSize(2);
        assertThat(second.getList("content.idType", Integer.class))
                .doesNotContainAnyElementsOf(first.getList("content.idType", Integer.class));
    }

    @Test
    @DisplayName("Paginates filtered results")
    void shouldPaginateFilteredResults_whenQueryAndSizeAreGiven() {
        JsonPath body = search(Map.of("query", "lav", "page", 0, "size", 2));

        assertThat(body.getList("content")).hasSize(2);
        assertThat(body.getInt("totalElements")).isEqualTo(3);
        assertThat(body.getInt("totalPages")).isEqualTo(2);
        assertThat(body.getBoolean("last")).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\" is stored as \"{1}\"")
    @CsvSource({
        "Lava Louça,lava louça",
        "'  Aspirador de Pó  ',aspirador de pó",
        "SECADOR,secador",
        "Aparelho de   Som,aparelho de som"
    })
    @DisplayName("Creates a type with a normalized name")
    void shouldCreateType_whenNameIsNew(String raw, String normalized) {
        int before = countTypes();

        JsonPath body = create(Map.of("type", raw), HttpStatus.CREATED);

        assertThat(body.getString("type")).isEqualTo(normalized);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT type FROM types WHERE id = ?",
                                String.class,
                                body.getInt("idType")))
                .isEqualTo(normalized);
        assertThat(countTypes()).isEqualTo(before + 1);
    }

    @ParameterizedTest(name = "\"{0}\" returns the existing type")
    @ValueSource(strings = {"Lavadora", "lavadora", "LAVADORA", "  Lavadora  "})
    @DisplayName("Create is idempotent for existing names")
    void shouldReturnExistingType_whenNameAlreadyExists(String raw) {
        int before = countTypes();

        JsonPath body = create(Map.of("type", raw), HttpStatus.CREATED);

        assertThat(body.getInt("idType")).isEqualTo(lavadoraId);
        assertThat(body.getString("type")).isEqualTo("lavadora");
        assertThat(countTypes()).isEqualTo(before);
    }

    @Test
    @DisplayName("Rejects an empty body")
    void shouldReturn400_whenBodyIsEmpty() {
        create(new HashMap<>(), HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "blank type \"{0}\"")
    @ValueSource(strings = {"", "  "})
    @DisplayName("Rejects a blank type name")
    void shouldReturn400_whenTypeIsBlank(String type) {
        int before = countTypes();

        JsonPath body = create(Map.of("type", type), HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(countTypes()).isEqualTo(before);
    }

    private JsonPath search(Map<String, Object> params) {
        return given().spec(getAuthenticatedSpec())
                .body(params)
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();
    }

    private JsonPath create(Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private int countTypes() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM types", Integer.class);
    }
}
