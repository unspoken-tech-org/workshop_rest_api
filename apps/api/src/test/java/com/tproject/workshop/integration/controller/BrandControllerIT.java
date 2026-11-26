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

class BrandControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/brand";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;
    private int samsungId;

    @BeforeEach
    void seedCatalog() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        samsungId = fixtures.brand("Samsung");
        for (String brand :
                List.of("LG", "LG Pro", "Brastemp", "Electrolux", "Consul", "Panasonic", "Midea")) {
            fixtures.brand(brand);
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
    @DisplayName("Empty query lists every brand with default pagination")
    void shouldListAllBrands_whenQueryIsEmpty() {
        JsonPath body = search(Map.of());

        assertThat(body.getList("content.brand", String.class))
                .containsExactlyInAnyOrder(
                        "samsung",
                        "lg",
                        "lg pro",
                        "brastemp",
                        "electrolux",
                        "consul",
                        "panasonic",
                        "midea");
        assertThat(body.getInt("totalElements")).isEqualTo(8);
        assertThat(body.getInt("totalPages")).isEqualTo(1);
        assertThat(body.getInt("size")).isEqualTo(15);
        assertThat(body.getBoolean("last")).isTrue();
    }

    @ParameterizedTest(name = "query \"{0}\" -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "sam|samsung",
                "SAM|samsung",
                "LG|lg,lg pro",
                "brast|brastemp",
                "electro|electrolux",
                "xyz123|"
            })
    @DisplayName("Search matches brands case-insensitively")
    void shouldFilterBrands_whenQueryIsGiven(String query, String expected) {
        JsonPath body = search(Map.of("query", query));

        List<String> expectedBrands = expected == null ? List.of() : List.of(expected.split(","));
        assertThat(body.getList("content.brand", String.class))
                .containsExactlyInAnyOrderElementsOf(expectedBrands);
        assertThat(body.getInt("totalElements")).isEqualTo(expectedBrands.size());
    }

    @Test
    @DisplayName("Search returns the persisted id of each brand")
    void shouldReturnBrandId_whenSearching() {
        JsonPath body = search(Map.of("query", "samsung"));

        assertThat(body.getList("content.idBrand", Integer.class)).containsExactly(samsungId);
    }

    @Test
    @DisplayName("Paginates results")
    void shouldPaginate_whenPageAndSizeAreGiven() {
        JsonPath firstPage = search(Map.of("page", 0, "size", 2));
        JsonPath lastPage = search(Map.of("page", 3, "size", 2));

        assertThat(firstPage.getList("content")).hasSize(2);
        assertThat(firstPage.getInt("totalElements")).isEqualTo(8);
        assertThat(firstPage.getInt("totalPages")).isEqualTo(4);
        assertThat(firstPage.getBoolean("last")).isFalse();
        assertThat(lastPage.getList("content")).hasSize(2);
        assertThat(lastPage.getBoolean("last")).isTrue();
    }

    @Test
    @DisplayName("Paginates filtered results")
    void shouldPaginateFilteredResults_whenQueryAndSizeAreGiven() {
        JsonPath body = search(Map.of("query", "LG", "page", 0, "size", 1));

        assertThat(body.getList("content")).hasSize(1);
        assertThat(body.getInt("totalElements")).isEqualTo(2);
        assertThat(body.getInt("totalPages")).isEqualTo(2);
        assertThat(body.getBoolean("last")).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\" is stored as \"{1}\"")
    @CsvSource({"Whirlpool,whirlpool", "'  Bosch  ',bosch", "Sharp   Corp,sharp corp"})
    @DisplayName("Creates a brand with a normalized name")
    void shouldCreateBrand_whenNameIsNew(String raw, String normalized) {
        int before = countBrands();

        JsonPath body = create(Map.of("brand", raw), HttpStatus.CREATED);

        assertThat(body.getString("brand")).isEqualTo(normalized);
        Integer id = body.getInt("idBrand");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT brand FROM brands WHERE id = ?", String.class, id))
                .isEqualTo(normalized);
        assertThat(countBrands()).isEqualTo(before + 1);
    }

    @ParameterizedTest(name = "\"{0}\" returns the existing brand")
    @ValueSource(strings = {"Samsung", "samsung", "SAMSUNG", "  Samsung  "})
    @DisplayName("Create is idempotent for existing names")
    void shouldReturnExistingBrand_whenNameAlreadyExists(String raw) {
        int before = countBrands();

        JsonPath body = create(Map.of("brand", raw), HttpStatus.CREATED);

        assertThat(body.getInt("idBrand")).isEqualTo(samsungId);
        assertThat(body.getString("brand")).isEqualTo("samsung");
        assertThat(countBrands()).isEqualTo(before);
    }

    @Test
    @DisplayName("Rejects an empty body")
    void shouldReturn400_whenBodyIsEmpty() {
        create(new HashMap<>(), HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "blank brand \"{0}\"")
    @ValueSource(strings = {"", "  "})
    @DisplayName("Rejects a blank brand name")
    void shouldReturn400_whenBrandIsBlank(String brand) {
        int before = countBrands();

        JsonPath body = create(Map.of("brand", brand), HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(countBrands()).isEqualTo(before);
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

    private int countBrands() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM brands", Integer.class);
    }
}
