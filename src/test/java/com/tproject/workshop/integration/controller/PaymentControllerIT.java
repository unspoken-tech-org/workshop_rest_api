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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(value = {"/test-scripts/cleanTestData.sql", "/test-scripts/PaymentControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class PaymentControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/payment";

    @Order(1)
    @DisplayName("Create payment")
    @MethodSource("createPaymentArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void createPayment(int index, Integer statusCode, Map<String, Object> arguments, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .contentType("application/json")
                .body(arguments)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes(index, response, List.of("paymentId", "paymentDate"));
    }

    private static Stream<Arguments> createPaymentArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, Map.of(
                        "deviceId", 1,
                        "paymentType", "credito",
                        "value", 150.00,
                        "category", "parcial"
                ), "create valid payment")
                , Arguments.of(2, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 2,
                        "paymentType", "invalid_type",
                        "value", 150.00,
                        "category", "parcial"
                ), "create payment with invalid type")
                , Arguments.of(3, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "paymentType", "credito",
                        "value", 0,
                        "category", "parcial"
                ), "create payment with value equals zero")
                , Arguments.of(4, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "value", 150,
                        "category", "parcial"
                ), "missing paymentType")
                , Arguments.of(5, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "paymentType", "credito",
                        "value", 150.00,
                        "category", "parcial"
                ), "missing deviceId")
        );
    }
}