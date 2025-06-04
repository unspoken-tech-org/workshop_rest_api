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

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/DeviceControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class DeviceControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/device";

    @Order(1)
    @DisplayName("List devices")
    @MethodSource("listDevicesArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void listDevices(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .post(BASE_PATH + "/filter")
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> listDevicesArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, Map.of(), "get all Devices")
        );
    }

    @Order(2)
    @DisplayName("Get device")
    @MethodSource("getDeviceArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void getDevice(int index, Integer statusCode, int id, String reason) {
        Response response = given().spec(SPEC)
            .when()
            .get(BASE_PATH + "/" + id)
            .then()
            .statusCode(statusCode)
            .extract()
            .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> getDeviceArguments() {
        return Stream.of(
            Arguments.of(1, HttpStatus.SC_OK, 1, "get single device")
        );
    }

}
