package com.tproject.workshop.integration.controller;


import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/CustomerControllerIT.script.sql"})
public class CustomerControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/customer";

    @Order(1)
    @DisplayName("List customers")
    @MethodSource("listCustomersArguments")
    @ParameterizedTest(name = "{displayName} {0}: role {1} status {2} body {3} reason {4}")
    public void listCustomers(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .queryParams(params)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> listCustomersArguments() {
        return Stream.of(
                Arguments.of(0, HttpStatus.SC_OK, Map.of(), "get all Customers")
        );
    }
}
