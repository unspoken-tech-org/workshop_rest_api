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
                Arguments.of(1, HttpStatus.SC_OK, 1, "get single device"),
                Arguments.of(2, HttpStatus.SC_NOT_FOUND, 9999, "get non existent device")
        );
    }

    @Order(3)
    @DisplayName("Create device")
    @MethodSource("createDeviceArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void createDevice(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .body(params)
                .post(BASE_PATH + "/create")
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
                        "typeBrandModel", Map.of(
                                "type", "Teste de tipo",
                                "brand", "Teste de marca",
                                "model", "Teste de modelo"
                        ),
                        "colors", List.of("Teste de cor"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with new type, brand and model"),
                Arguments.of(2, HttpStatus.SC_CREATED, Map.of(
                        "customerId", 1,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with existing type, brand, model and color"),
                Arguments.of(3, HttpStatus.SC_CREATED, Map.of(
                        "customerId", 1,
                        "technicianId", 1,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with existing technician"),
                Arguments.of(4, HttpStatus.SC_NOT_FOUND, Map.of(
                        "customerId", 9999,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with inexistent customer"),
                Arguments.of(5, HttpStatus.SC_NOT_FOUND, Map.of(
                        "customerId", 1,
                        "technicianId", 9999,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with inexistent technician"),
                Arguments.of(6, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with null customer"),
                Arguments.of(7, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "customerId", 1,
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with null typeBrandModel"),
                Arguments.of(8, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "customerId", 1,
                        "typeBrandModel", Map.of(
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with type null"),
                Arguments.of(9, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "customerId", 1,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with model null"),
                Arguments.of(10, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "customerId", 1,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with brand null"),
                Arguments.of(11, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "customerId", 1,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "colors", List.of("Azul"),
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with null problem"),
                Arguments.of(12, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "customerId", 1,
                        "typeBrandModel", Map.of(
                                "type", "Ventilador",
                                "brand", "Arno",
                                "model", "Model 1"
                        ),
                        "problem", "Teste de problema",
                        "observation", "Teste de observação",
                        "budgetValue", 100.0,
                        "hasUrgency", true
                ), "create device with colors null")


        );
    }
}
