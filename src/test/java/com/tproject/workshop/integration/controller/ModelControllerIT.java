package com.tproject.workshop.integration.controller;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql",
        "/test-scripts/ModelControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class ModelControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/model";

    @DisplayName("Search models")
    @MethodSource("searchModelsArguments")
    @ParameterizedTest(name = "{displayName} : {0} {2}")
    public void searchModels(int index, Map<String, Object> params, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .body(params)
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> searchModelsArguments() {
        return Stream.of(
                // typeId=1 (Lavadora), brandId=1 (Samsung): WT12345, WT12300, WT500X
                Arguments.of(1, Map.of("typeId", 1, "brandId", 1), "get all models for Samsung Lavadora (empty query)"),
                Arguments.of(2, Map.of("typeId", 1, "brandId", 1, "query", "wt123"), "search 'wt123' in Samsung Lavadora"),
                Arguments.of(3, Map.of("typeId", 1, "brandId", 1, "query", "WT123"), "search 'WT123' uppercase"),
                Arguments.of(4, Map.of("typeId", 1, "brandId", 1, "query", "500"), "search '500' in Samsung Lavadora"),
                Arguments.of(5, Map.of("typeId", 1, "brandId", 1, "query", "sm"), "search 'sm' in Samsung Lavadora"),

                // typeId=1 (Lavadora), brandId=2 (LG): WT12345
                Arguments.of(6, Map.of("typeId", 1, "brandId", 2), "get all models for LG Lavadora"),
                Arguments.of(7, Map.of("typeId", 1, "brandId", 2, "query", "wt"), "search 'wt' in LG Lavadora"),

                // typeId=2 (Micro-ondas), brandId=3 (Panasonic): NN-ST25, NN-SD45, MS-TRL52
                Arguments.of(8, Map.of("typeId", 2, "brandId", 3), "get all models for Panasonic Micro-ondas"),
                Arguments.of(9, Map.of("typeId", 2, "brandId", 3, "query", "nn"), "search 'nn' in Panasonic Micro-ondas"),
                Arguments.of(10, Map.of("typeId", 2, "brandId", 3, "query", "ms"), "search 'ms' in Panasonic Micro-ondas"),
                Arguments.of(11, Map.of("typeId", 2, "brandId", 3, "query", "25"), "search '25' in Panasonic Micro-ondas"),

                // Non-existent brand+type
                Arguments.of(12, Map.of("typeId", 1, "brandId", 999), "non-existent brand returns empty"),

                // Non-existent model
                Arguments.of(13, Map.of("typeId", 1, "brandId", 1, "query", "xyz123"), "non-existent model returns empty"),

                // Pagination
                Arguments.of(14, Map.of("typeId", 1, "brandId", 1, "page", 0, "size", 2), "pagination first page size 2"),
                Arguments.of(15, Map.of("typeId", 2, "brandId", 3, "query", "nn", "page", 0, "size", 1), "filtered pagination")
        );
    }

    @DisplayName("Create model")
    @MethodSource("createModelArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} {2}")
    public void createModel(int index, Integer statusCode, Map<String, Object> body, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> createModelArguments() {
        return Stream.of(
                Arguments.of(16, HttpStatus.SC_CREATED, Map.of("model", "WT99999"), "create new model"),
                Arguments.of(17, HttpStatus.SC_CREATED, Map.of("model", "  SM-New  "), "create model with whitespace"),
                Arguments.of(18, HttpStatus.SC_CREATED, Map.of("model", "NN-K15"), "create another model")
        );
    }

    @DisplayName("Create model - idempotent (returns existing)")
    @MethodSource("createModelExistingArguments")
    @ParameterizedTest(name = "{displayName} : {0} {1}")
    public void createModelExisting(int index, Map<String, Object> body, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> createModelExistingArguments() {
        return Stream.of(
                Arguments.of(19, Map.of("model", "WT12345"), "existing model returns same"),
                Arguments.of(20, Map.of("model", "wt12345"), "existing model case insensitive"),
                Arguments.of(21, Map.of("model", "NN-ST25"), "existing model another")
        );
    }
}
