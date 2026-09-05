package com.tproject.workshop.integration.controller;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql",
        "/test-scripts/TypeControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class TypeControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/type";

    @DisplayName("Search types")
    @MethodSource("searchTypesArguments")
    @ParameterizedTest(name = "{displayName} : {0} {2}")
    public void searchTypes(int index, Map<String, Object> params, String reason) {
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

    private static Stream<Arguments> searchTypesArguments() {
        return Stream.of(
                Arguments.of(1, Map.of(), "get all types (empty query)"),
                Arguments.of(2, Map.of("query", "lav"), "search types containing 'lav'"),
                Arguments.of(3, Map.of("query", "LAV"), "search types case insensitive"),
                Arguments.of(4, Map.of("query", "micro"), "search types containing 'micro'"),
                Arguments.of(5, Map.of("query", "refrigerador"), "search by exact type name"),
                Arguments.of(6, Map.of("query", "xyz123"), "search non-existent type returns empty"),
                Arguments.of(7, Map.of("query", "la"), "partial match 'la'"),
                Arguments.of(8, Map.of("query", "e seca"), "multi-word search 'e seca'"),
                Arguments.of(9, Map.of("query", "industrial"), "search 'industrial'"),
                Arguments.of(10, Map.of("page", 0, "size", 2), "pagination first page size 2"),
                Arguments.of(11, Map.of("page", 1, "size", 2), "pagination second page size 2"),
                Arguments.of(12, Map.of("query", "lav", "page", 0, "size", 2), "filtered pagination")
        );
    }

    @DisplayName("Create type")
    @MethodSource("createTypeArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} {2}")
    public void createType(int index, Integer statusCode, Map<String, Object> body, String reason) {
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

    private static Stream<Arguments> createTypeArguments() {
        return Stream.of(
                Arguments.of(13, HttpStatus.SC_CREATED, Map.of("type", "Lava Louça"), "create new type"),
                Arguments.of(14, HttpStatus.SC_CREATED, Map.of("type", "  Aspirador  "), "create type with whitespace"),
                Arguments.of(15, HttpStatus.SC_CREATED, Map.of("type", "SECADOR"), "create type uppercase"),
                Arguments.of(16, HttpStatus.SC_CREATED, Map.of("type", "Aparelho de Som"), "create another new type")
        );
    }

    @DisplayName("Create type - idempotent (returns existing)")
    @MethodSource("createTypeExistingArguments")
    @ParameterizedTest(name = "{displayName} : {0} {1}")
    public void createTypeExisting(int index, Map<String, Object> body, String reason) {
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

    private static Stream<Arguments> createTypeExistingArguments() {
        return Stream.of(
                Arguments.of(17, Map.of("type", "Lavadora"), "existing type returns same"),
                Arguments.of(18, Map.of("type", "lavadora"), "existing type case insensitive"),
                Arguments.of(19, Map.of("type", "LAVADORA"), "existing type all uppercase"),
                Arguments.of(20, Map.of("type", "  Lavadora  "), "existing type with whitespace")
        );
    }

    @DisplayName("Create type - validation")
    @MethodSource("createTypeValidationArguments")
    @ParameterizedTest(name = "{displayName} : status {0} {1}")
    public void createTypeValidation(Integer statusCode, Map<String, Object> body, String reason) {
        given().spec(getAuthenticatedSpec())
                .when()
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode);
    }

    private static Stream<Arguments> createTypeValidationArguments() {
        return Stream.of(
                Arguments.of(HttpStatus.SC_BAD_REQUEST, new HashMap<>(), "empty body"),
                Arguments.of(HttpStatus.SC_BAD_REQUEST, Map.of("type", ""), "blank type"),
                Arguments.of(HttpStatus.SC_BAD_REQUEST, Map.of("type", "  "), "whitespace-only type")
        );
    }
}
