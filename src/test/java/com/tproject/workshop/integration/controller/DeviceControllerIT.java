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
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/DeviceControllerIT.script.sql"})
public class DeviceControllerIT  extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/device";

    @Order(1)
    @DisplayName("List devices")
    @MethodSource("listDevicesArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void listCustomers(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> listDevicesArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, Map.of(), "get all Customers")
        );
    }

    @Order(2)
    @DisplayName("List devices")
    @MethodSource("listDevicesTableArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void listDevicesTable(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .queryParams(params)
                .get(BASE_PATH + "/table")
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> listDevicesTableArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, Map.of(), "get all table devices"),
                Arguments.of(2, HttpStatus.SC_OK, Map.of("deviceIds", List.of(1)), "get table devices by deviceIds"),
                Arguments.of(3, HttpStatus.SC_OK, Map.of("customerName","Alfonso"), "get table devices by customer name")

        );
    }


    @Order(3)
    @DisplayName("create device")
    @MethodSource("createDeviceArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void createDevice(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .body(params)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> createDeviceArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_CREATED, Map.of(
                        "customerId", 1,
                        "deviceStatusId", 1,
                        "brandId", 1,
                        "modelId", 1,
                        "typeId", 1,
                        "technicianId", 1,
                        "problem", "problem",
                        "observation", "observation",
                        "hasUrgency", false
                ), "create device")
        );
    }
}
