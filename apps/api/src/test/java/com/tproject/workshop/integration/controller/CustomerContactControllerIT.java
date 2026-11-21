package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({
    "/test-scripts/cleanTestData.sql",
    "/test-scripts/AuthSetup.sql",
    "/test-scripts/CustomerContactControllerIT.script.sql",
    "/test-scripts/resetTablesSequence.sql"
})
public class CustomerContactControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/customer-contact";

    @Order(0)
    @DisplayName("Reject access without JWT")
    @Test
    public void shouldReturn401_whenNoJwt() {
        given().spec(SPEC)
                .when()
                .get(BASE_PATH + "/1")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(1)
    @DisplayName("Save customer contact")
    @MethodSource("saveCustomerContactArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void saveCustomerContact(
            int index, Integer statusCode, Map<String, Object> arguments, String reason) {
        Response response =
                given().spec(getAuthenticatedSpec())
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
                Arguments.of(
                        1,
                        HttpStatus.SC_CREATED,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "create valid customer contact"),
                Arguments.of(
                        2,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing deviceId"),
                Arguments.of(
                        3,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing technicianId"),
                Arguments.of(
                        4,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing phoneNumber"),
                Arguments.of(
                        5,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "empty message"),
                Arguments.of(
                        6,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing message"),
                Arguments.of(
                        7,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing contactStatus"),
                Arguments.of(
                        8,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing deviceStatus"),
                Arguments.of(
                        9,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue"),
                        "missing contactDate"),
                Arguments.of(
                        10,
                        HttpStatus.SC_NOT_FOUND,
                        Map.of(
                                "deviceId", 999,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste service fail",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent deviceId"),
                Arguments.of(
                        11,
                        HttpStatus.SC_NOT_FOUND,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 999,
                                "phoneNumber", "44987654321",
                                "message", "teste service fail",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent technicianId"),
                Arguments.of(
                        12,
                        HttpStatus.SC_NOT_FOUND,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "99999999999",
                                "message", "teste service fail",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent phoneNumber"),
                Arguments.of(
                        13,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste service fail",
                                "contactStatus", true,
                                "deviceStatus", "non_existent_status",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent deviceStatus"),
                Arguments.of(
                        14,
                        HttpStatus.SC_BAD_REQUEST,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "9999",
                                "message", "teste service fail",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "phoneNumber does not have min 10 digits"),
                Arguments.of(
                        15,
                        HttpStatus.SC_CREATED,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "(44) 98765-4321",
                                "message", "teste service fail",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "phoneNumber with special characters"));
    }

    @Order(2)
    @DisplayName("Update customer contact")
    @MethodSource("updateCustomerContactArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} id {2} body {3} reason {4}")
    public void updateCustomerContact(
            int index,
            Integer statusCode,
            Integer id,
            Map<String, Object> arguments,
            String reason) {
        Response response =
                given().spec(getAuthenticatedSpec())
                        .when()
                        .contentType("application/json")
                        .body(arguments)
                        .put(BASE_PATH + "/{id}", id)
                        .then()
                        .statusCode(statusCode)
                        .extract()
                        .response();

        super.validateResponseIgnoreAttributes(index, response, List.of("lastContact"));
    }

    private static Stream<Arguments> updateCustomerContactArguments() {
        return Stream.of(
                Arguments.of(
                        1,
                        HttpStatus.SC_OK,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "updated message",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "update all fields"),
                Arguments.of(
                        2,
                        HttpStatus.SC_OK,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "WHATSAPP",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "device is ready",
                                "contactStatus", true,
                                "deviceStatus", "PRONTO",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "update device status to PRONTO"),
                Arguments.of(
                        3,
                        HttpStatus.SC_BAD_REQUEST,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing phoneNumber for mensagem"),
                Arguments.of(
                        4,
                        HttpStatus.SC_BAD_REQUEST,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "non_existent_status",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "invalid device status"),
                Arguments.of(
                        5,
                        HttpStatus.SC_BAD_REQUEST,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "invalid_date"),
                        "invalid date format"),
                Arguments.of(
                        6,
                        HttpStatus.SC_BAD_REQUEST,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "9999",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "phoneNumber less than 10 digits"),
                Arguments.of(
                        7,
                        HttpStatus.SC_NOT_FOUND,
                        999,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent contact id"),
                Arguments.of(
                        8,
                        HttpStatus.SC_NOT_FOUND,
                        1,
                        Map.of(
                                "deviceId", 999,
                                "contactType", "mensagem",
                                "technicianId", 2,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent device id"),
                Arguments.of(
                        9,
                        HttpStatus.SC_NOT_FOUND,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "mensagem",
                                "technicianId", 999,
                                "phoneNumber", "44987654321",
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "non-existent technician id"),
                Arguments.of(
                        10,
                        HttpStatus.SC_BAD_REQUEST,
                        1,
                        Map.of(
                                "deviceId", 1,
                                "contactType", "ligacao",
                                "technicianId", 2,
                                "message", "teste",
                                "contactStatus", true,
                                "deviceStatus", "entregue",
                                "contactDate", "2025-06-03T23:08:14.110245"),
                        "missing phoneNumber for ligacao"));
    }

    @Order(3)
    @DisplayName("Only the latest contact changes the current device status")
    @Test
    public void shouldOnlyChangeDeviceStatus_whenContactIsTheLatest() {
        // Contato mais recente (2025) marca o aparelho como ENTREGUE.
        postContact(contactBody("entregue", "2025-06-03T10:00:00"), HttpStatus.SC_CREATED);
        assertDeviceStatus("ENTREGUE");

        // Corrigir o contato antigo (id 1, 2024) não reverte o aparelho.
        given().spec(getAuthenticatedSpec())
                .contentType("application/json")
                .body(contactBody("em_andamento", "2024-01-10T09:00:00"))
                .put(BASE_PATH + "/{id}", 1)
                .then()
                .statusCode(HttpStatus.SC_OK);
        assertDeviceStatus("ENTREGUE");

        // Registrar um contato retroativo também não altera o status atual.
        postContact(contactBody("aguardando", "2023-01-01T09:00:00"), HttpStatus.SC_CREATED);
        assertDeviceStatus("ENTREGUE");

        // Alterar o status do contato mais recente (id 2) reflete no aparelho.
        given().spec(getAuthenticatedSpec())
                .contentType("application/json")
                .body(contactBody("pronto", "2025-06-03T10:00:00"))
                .put(BASE_PATH + "/{id}", 2)
                .then()
                .statusCode(HttpStatus.SC_OK);
        assertDeviceStatus("PRONTO");
    }

    private void postContact(Map<String, Object> body, int expectedStatus) {
        given().spec(getAuthenticatedSpec())
                .contentType("application/json")
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(expectedStatus);
    }

    private void assertDeviceStatus(String expected) {
        String status =
                jdbcTemplate.queryForObject(
                        "SELECT device_status FROM devices WHERE id = 1", String.class);
        assertThat(status).isEqualTo(expected);
    }

    private static Map<String, Object> contactBody(String deviceStatus, String contactDate) {
        return Map.of(
                "deviceId",
                1,
                "contactType",
                "pessoalmente",
                "technicianId",
                2,
                "message",
                "teste",
                "contactStatus",
                true,
                "deviceStatus",
                deviceStatus,
                "contactDate",
                contactDate);
    }
}
