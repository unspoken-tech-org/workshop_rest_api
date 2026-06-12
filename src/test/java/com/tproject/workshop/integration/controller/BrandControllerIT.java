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
        "/test-scripts/BrandControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class BrandControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/brand";

    @DisplayName("Search brands")
    @MethodSource("searchBrandsArguments")
    @ParameterizedTest(name = "{displayName} : {0} {2}")
    public void searchBrands(int index, Map<String, Object> params, String reason) {
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

    private static Stream<Arguments> searchBrandsArguments() {
        return Stream.of(
                // All brands (empty query)
                Arguments.of(1, Map.of(), "get all brands (empty query)"),
                Arguments.of(2, Map.of("query", "sam"), "search 'sam'"),
                Arguments.of(3, Map.of("query", "SAM"), "search 'SAM' uppercase"),
                Arguments.of(4, Map.of("query", "LG"), "search 'LG'"),
                Arguments.of(5, Map.of("query", "brast"), "search 'brast' partial"),
                Arguments.of(6, Map.of("query", "electro"), "search 'electro'"),

                // Non-existent brand
                Arguments.of(16, Map.of("query", "xyz123"), "non-existent brand returns empty"),

                // Pagination
                Arguments.of(17, Map.of("page", 0, "size", 2), "pagination first page size 2"),
                Arguments.of(18, Map.of("query", "LG", "page", 0, "size", 1), "filtered pagination")
        );
    }

    @DisplayName("Create brand")
    @MethodSource("createBrandArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} {2}")
    public void createBrand(int index, Integer statusCode, Map<String, Object> body, String reason) {
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

    private static Stream<Arguments> createBrandArguments() {
        return Stream.of(
                Arguments.of(19, HttpStatus.SC_CREATED, Map.of("brand", "Whirlpool"), "create new brand"),
                Arguments.of(20, HttpStatus.SC_CREATED, Map.of("brand", "  Bosch  "), "create brand with whitespace"),
                Arguments.of(21, HttpStatus.SC_CREATED, Map.of("brand", "Sharp"), "create another brand")
        );
    }

    @DisplayName("Create brand - idempotent (returns existing)")
    @MethodSource("createBrandExistingArguments")
    @ParameterizedTest(name = "{displayName} : {0} {1}")
    public void createBrandExisting(int index, Map<String, Object> body, String reason) {
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

    private static Stream<Arguments> createBrandExistingArguments() {
        return Stream.of(
                Arguments.of(22, Map.of("brand", "Samsung"), "existing brand returns same"),
                Arguments.of(23, Map.of("brand", "samsung"), "existing brand case insensitive"),
                Arguments.of(24, Map.of("brand", "LG"), "existing brand another")
        );
    }
}
