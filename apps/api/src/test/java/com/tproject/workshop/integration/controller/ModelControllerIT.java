package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class ModelControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/model";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;
    private int lavadoraId;
    private int microondasId;
    private int samsungId;
    private int lgId;
    private int panasonicId;
    private int wt12345Id;

    @BeforeEach
    void seedCatalog() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        lavadoraId = fixtures.type("Lavadora");
        microondasId = fixtures.type("Micro-ondas");
        samsungId = fixtures.brand("Samsung");
        lgId = fixtures.brand("LG");
        panasonicId = fixtures.brand("Panasonic");

        wt12345Id = link(samsungId, lavadoraId, "WT12345");
        link(samsungId, lavadoraId, "WT12300");
        link(samsungId, lavadoraId, "WT500X");
        link(samsungId, microondasId, "SM-L22");
        link(samsungId, microondasId, "SM-G991");
        link(lgId, lavadoraId, "WT12345");
        link(panasonicId, microondasId, "NN-ST25");
        link(panasonicId, microondasId, "NN-SD45");
        link(panasonicId, microondasId, "MS-TRL52");
    }

    private int link(int brandId, int typeId, String model) {
        int modelId = fixtures.model(model);
        fixtures.brandModelType(brandId, modelId, typeId);
        return modelId;
    }

    @Test
    @DisplayName("Rejects search without JWT")
    void shouldReturn401_whenSearchingWithoutJwt() {
        given().spec(SPEC)
                .body(Map.of("typeId", 1, "brandId", 1))
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Lists only the models linked to the brand and type")
    void shouldListModels_whenBrandAndTypeAreGiven() {
        JsonPath samsungWashers = search(samsungId, lavadoraId, null);
        JsonPath lgWashers = search(lgId, lavadoraId, null);
        JsonPath panasonicMicrowaves = search(panasonicId, microondasId, null);

        assertThat(samsungWashers.getList("content.model", String.class))
                .containsExactlyInAnyOrder("wt12345", "wt12300", "wt500x");
        assertThat(samsungWashers.getInt("totalElements")).isEqualTo(3);
        assertThat(lgWashers.getList("content.model", String.class)).containsExactly("wt12345");
        assertThat(panasonicMicrowaves.getList("content.model", String.class))
                .containsExactlyInAnyOrder("nn-st25", "nn-sd45", "ms-trl52");
    }

    @Test
    @DisplayName("Search returns the persisted id of each model")
    void shouldReturnModelId_whenSearching() {
        JsonPath body = search(lgId, lavadoraId, "wt12345");

        assertThat(body.getList("content.idModel", Integer.class)).containsExactly(wt12345Id);
    }

    @ParameterizedTest(name = "query \"{0}\" -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "wt123|wt12345,wt12300",
                "WT123|wt12345,wt12300",
                // the search is prefix based: "500" is not a prefix of "wt500x"
                "500|",
                // "sm-*" models belong to Samsung microwaves, not washers
                "sm|",
                "xyz123|"
            })
    @DisplayName("Filters Samsung washers by query")
    void shouldFilterModels_whenQueryIsGiven(String query, String expected) {
        JsonPath body = search(samsungId, lavadoraId, query);

        List<String> expectedModels = expected == null ? List.of() : List.of(expected.split(","));
        assertThat(body.getList("content.model", String.class))
                .containsExactlyInAnyOrderElementsOf(expectedModels);
        assertThat(body.getInt("totalElements")).isEqualTo(expectedModels.size());
    }

    @ParameterizedTest(name = "Panasonic microwaves, query \"{0}\" -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {"nn|nn-st25,nn-sd45", "ms|ms-trl52", "25|"})
    @DisplayName("Filters Panasonic microwaves by query")
    void shouldFilterModels_whenQueryMatchesOtherBrand(String query, String expected) {
        JsonPath body = search(panasonicId, microondasId, query);

        List<String> expectedModels = expected == null ? List.of() : List.of(expected.split(","));
        assertThat(body.getList("content.model", String.class))
                .containsExactlyInAnyOrderElementsOf(expectedModels);
    }

    @Test
    @DisplayName("Returns an empty page for a brand without models")
    void shouldReturnEmptyPage_whenBrandDoesNotExist() {
        JsonPath body = search(999_999, lavadoraId, null);

        assertThat(body.getList("content")).isEmpty();
        assertThat(body.getInt("totalElements")).isZero();
    }

    @Test
    @DisplayName("Paginates results")
    void shouldPaginate_whenPageAndSizeAreGiven() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("typeId", lavadoraId);
        params.put("brandId", samsungId);
        params.put("page", 0);
        params.put("size", 2);

        JsonPath first = post(params);
        params.put("page", 1);
        JsonPath second = post(params);

        assertThat(first.getList("content")).hasSize(2);
        assertThat(first.getInt("totalElements")).isEqualTo(3);
        assertThat(first.getInt("totalPages")).isEqualTo(2);
        assertThat(first.getBoolean("last")).isFalse();
        assertThat(second.getList("content")).hasSize(1);
        assertThat(second.getBoolean("last")).isTrue();
        Set<String> all = new HashSet<>(first.getList("content.model", String.class));
        all.addAll(second.getList("content.model", String.class));
        assertThat(all).containsExactlyInAnyOrder("wt12345", "wt12300", "wt500x");
    }

    @Test
    @DisplayName("Paginates filtered results")
    void shouldPaginateFilteredResults_whenQueryAndSizeAreGiven() {
        JsonPath body =
                post(
                        Map.of(
                                "typeId",
                                microondasId,
                                "brandId",
                                panasonicId,
                                "query",
                                "nn",
                                "page",
                                0,
                                "size",
                                1));

        assertThat(body.getList("content")).hasSize(1);
        assertThat(body.getInt("totalElements")).isEqualTo(2);
        assertThat(body.getInt("totalPages")).isEqualTo(2);
    }

    @Test
    @DisplayName("Rejects a search without typeId or brandId")
    void shouldReturn400_whenSearchHasNoBrandOrType() {
        given().spec(getAuthenticatedSpec())
                .body(Map.of("query", "wt"))
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @ParameterizedTest(name = "\"{0}\" is stored as \"{1}\"")
    @CsvSource({"WT99999,wt99999", "'  SM-New  ',sm-new", "NN-K15,nn-k15"})
    @DisplayName("Creates a model with a normalized name")
    void shouldCreateModel_whenNameIsNew(String raw, String normalized) {
        int before = countModels();

        JsonPath body = create(Map.of("model", raw), HttpStatus.CREATED);

        assertThat(body.getString("model")).isEqualTo(normalized);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT model FROM models WHERE id = ?",
                                String.class,
                                body.getInt("idModel")))
                .isEqualTo(normalized);
        assertThat(countModels()).isEqualTo(before + 1);
    }

    @ParameterizedTest(name = "\"{0}\" returns the existing model")
    @ValueSource(strings = {"WT12345", "wt12345", " Wt12345 "})
    @DisplayName("Create is idempotent for existing names")
    void shouldReturnExistingModel_whenNameAlreadyExists(String raw) {
        int before = countModels();

        JsonPath body = create(Map.of("model", raw), HttpStatus.CREATED);

        assertThat(body.getInt("idModel")).isEqualTo(wt12345Id);
        assertThat(countModels()).isEqualTo(before);
    }

    @Test
    @DisplayName("Rejects an empty body")
    void shouldReturn400_whenBodyIsEmpty() {
        create(new HashMap<>(), HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest(name = "blank model \"{0}\"")
    @ValueSource(strings = {"", "  "})
    @DisplayName("Rejects a blank model name")
    void shouldReturn400_whenModelIsBlank(String model) {
        int before = countModels();

        JsonPath body = create(Map.of("model", model), HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(countModels()).isEqualTo(before);
    }

    private JsonPath search(int brandId, int typeId, String query) {
        Map<String, Object> params = new HashMap<>();
        params.put("typeId", typeId);
        params.put("brandId", brandId);
        if (query != null) {
            params.put("query", query);
        }
        return post(params);
    }

    private JsonPath post(Map<String, Object> params) {
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

    private int countModels() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM models", Integer.class);
    }
}
