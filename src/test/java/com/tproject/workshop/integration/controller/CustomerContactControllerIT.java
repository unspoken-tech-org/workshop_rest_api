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
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/CustomerContactControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class CustomerContactControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/customer-contact";

    @Order(1)
    @DisplayName("Save customer contact")
    @MethodSource("saveCustomerContactArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void saveCustomerContact(int index, Integer statusCode, Map<String, Object> arguments, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .contentType("application/json")
                .body(arguments)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes(index, response, List.of("lastContact"));
    }

    private static Stream<Arguments> saveCustomerContactArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_CREATED, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "create valid customer contact"),
                Arguments.of(2, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "missing deviceId"),
                Arguments.of(3, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "phoneNumber", "44987654321",
                        "message", "teste",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "missing technicianId"),
                Arguments.of(4, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "message", "teste",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "missing phoneNumber"),
                Arguments.of(5, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "empty message"),
                Arguments.of(6, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "missing message"),
                Arguments.of(7, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste",
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "missing contactStatus"),
                Arguments.of(8, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste",
                        "contactStatus", true,
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "missing deviceStatus"),
                Arguments.of(9, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste",
                        "contactStatus", true,
                        "deviceStatus", "entregue"
                ), "missing contactDate"),
                Arguments.of(10, HttpStatus.SC_NOT_FOUND, Map.of(
                        "deviceId", 999,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste service fail",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "non-existent deviceId"),
                Arguments.of(11, HttpStatus.SC_NOT_FOUND, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 999,
                        "phoneNumber", "44987654321",
                        "message", "teste service fail",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "non-existent technicianId"),
                Arguments.of(12, HttpStatus.SC_NOT_FOUND, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "99999999999",
                        "message", "teste service fail",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "non-existent phoneNumber"),
                Arguments.of(13, HttpStatus.SC_NOT_FOUND, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "44987654321",
                        "message", "teste service fail",
                        "contactStatus", true,
                        "deviceStatus", "non_existent_status",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "non-existent deviceStatus"),
                Arguments.of(14, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "9999",
                        "message", "teste service fail",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "phoneNumber does not have min 10 digits"),
                Arguments.of(15, HttpStatus.SC_CREATED, Map.of(
                        "deviceId", 1,
                        "contactType", "mensagem",
                        "technicianId", 2,
                        "phoneNumber", "(44) 98765-4321",
                        "message", "teste service fail",
                        "contactStatus", true,
                        "deviceStatus", "entregue",
                        "contactDate", "2025-06-03T23:08:14.110245"
                ), "phoneNumber with special characters")
        );
    }
}
