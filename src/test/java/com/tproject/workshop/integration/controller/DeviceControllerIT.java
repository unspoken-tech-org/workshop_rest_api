package com.tproject.workshop.integration.controller;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/DeviceControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class DeviceControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/device";

    @Order(1)
    @DisplayName("search devices")
    @MethodSource("searchDevicesArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void searchDevices(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .body(params)
                .post(BASE_PATH + "/filter")
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> searchDevicesArguments() {
        return Stream.of(
                // Basic search tests
                Arguments.of(1, HttpStatus.SC_OK, Map.of(), "get all devices"),

                // Filter by device ID
                Arguments.of(2, HttpStatus.SC_OK, Map.of("deviceId", 1), "get device by specific ID"),
                Arguments.of(3, HttpStatus.SC_OK, Map.of("deviceId", 9999), "search for non-existent device ID"),

                // Filter by customer information
                Arguments.of(4, HttpStatus.SC_OK, Map.of("customerName", "Alfonso"), "get devices by customer name"),
                Arguments.of(5, HttpStatus.SC_OK, Map.of("customerName", "ALF"), "get devices by partial customer name uppercase"),
                Arguments.of(6, HttpStatus.SC_OK, Map.of("customerName", "zimmer"), "get devices by customer last name"),
                Arguments.of(7, HttpStatus.SC_OK, Map.of("customerCpf", "31781477051"), "get devices by customer CPF"),
                Arguments.of(8, HttpStatus.SC_OK, Map.of("customerPhone", "4430356678"), "get devices by customer phone"),

                // Filter by status
                Arguments.of(9, HttpStatus.SC_OK, Map.of("status", List.of("NOVO")), "get devices with NOVO status"),
                Arguments.of(10, HttpStatus.SC_OK, Map.of("status", List.of("EM_ANDAMENTO")), "get devices with EM_ANDAMENTO status"),
                Arguments.of(11, HttpStatus.SC_OK, Map.of("status", List.of("AGUARDANDO")), "get devices with AGUARDANDO status"),
                Arguments.of(12, HttpStatus.SC_OK, Map.of("status", List.of("ENTREGUE")), "get devices with ENTREGUE status"),
                Arguments.of(13, HttpStatus.SC_OK, Map.of("status", List.of("DESCARTADO")), "get devices with DESCARTADO status"),
                Arguments.of(14, HttpStatus.SC_OK, Map.of("status", List.of("NOVO", "EM_ANDAMENTO")), "get devices with multiple statuses"),

                // Filter by type IDs (matching the brands_models_types table)
                Arguments.of(15, HttpStatus.SC_OK, Map.of("deviceTypes", List.of(1)), "get devices by type ID (Ventilador)"),
                Arguments.of(16, HttpStatus.SC_OK, Map.of("deviceTypes", List.of(2)), "get devices by type ID (Liquidificador)"),
                Arguments.of(17, HttpStatus.SC_OK, Map.of("deviceTypes", List.of(1, 2)), "get devices by multiple type IDs"),

                // Filter by brand IDs
                Arguments.of(18, HttpStatus.SC_OK, Map.of("deviceBrands", List.of(1)), "get devices by brand ID (Arno)"),
                Arguments.of(19, HttpStatus.SC_OK, Map.of("deviceBrands", List.of(2)), "get devices by brand ID (Wallita)"),
                Arguments.of(20, HttpStatus.SC_OK, Map.of("deviceBrands", List.of(1, 2)), "get devices by multiple brand IDs"),

                // Filter by urgency and revision flags
                Arguments.of(21, HttpStatus.SC_OK, Map.of("urgency", true), "get devices with urgency"),
                Arguments.of(22, HttpStatus.SC_OK, Map.of("urgency", false), "get devices without urgency"),
                Arguments.of(23, HttpStatus.SC_OK, Map.of("revision", true), "get devices with revision"),
                Arguments.of(24, HttpStatus.SC_OK, Map.of("revision", false), "get devices without revision"),
                Arguments.of(25, HttpStatus.SC_OK, Map.of("urgency", true, "revision", true), "get devices with urgency AND revision"),

                // Filter by date range (format: yyyy-MM-dd)
                Arguments.of(26, HttpStatus.SC_OK, Map.of("initialEntryDate", "2021-01-01"), "get devices from specific initial date"),
                Arguments.of(27, HttpStatus.SC_OK, Map.of("finalEntryDate", "2023-12-31"), "get devices until specific final date"),
                Arguments.of(28, HttpStatus.SC_OK, Map.of(
                        "initialEntryDate", "2023-01-01",
                        "finalEntryDate", "2023-12-31"
                ), "get devices in date range 2023"),
                Arguments.of(29, HttpStatus.SC_OK, Map.of(
                        "initialEntryDate", "2021-04-01",
                        "finalEntryDate", "2021-04-30"
                ), "get devices in April 2021"),

                // Pagination tests
                Arguments.of(30, HttpStatus.SC_OK, Map.of("page", 0, "size", 5), "pagination first page with size 5"),
                Arguments.of(31, HttpStatus.SC_OK, Map.of("page", 1, "size", 5), "pagination second page with size 5"),
                Arguments.of(32, HttpStatus.SC_OK, Map.of("page", 2, "size", 5), "pagination third page with size 5"),
                Arguments.of(33, HttpStatus.SC_OK, Map.of("page", 0, "size", 3), "pagination with smaller page size"),
                Arguments.of(34, HttpStatus.SC_OK, Map.of("page", 10, "size", 5), "pagination beyond available data"),

                // Complex combination filters
                Arguments.of(35, HttpStatus.SC_OK, Map.of(
                        "customerName", "Alfonso",
                        "status", List.of("NOVO", "EM_ANDAMENTO")
                ), "filter by customer name and status combination"),
                Arguments.of(36, HttpStatus.SC_OK, Map.of(
                        "deviceTypes", List.of(1), // Ventilador
                        "urgency", true
                ), "filter by type and urgency combination"),
                Arguments.of(37, HttpStatus.SC_OK, Map.of(
                        "deviceBrands", List.of(1, 2), // Arno, Wallita
                        "status", List.of("ENTREGUE"),
                        "revision", false
                ), "filter by brands, status and revision combination"),

                // Edge cases - empty results
                Arguments.of(38, HttpStatus.SC_OK, Map.of("customerName", "NonExistentCustomer"), "search with non-existent customer name"),
                Arguments.of(39, HttpStatus.SC_OK, Map.of("customerCpf", "00000000000"), "search with non-existent CPF"),
                Arguments.of(40, HttpStatus.SC_OK, Map.of("status", List.of("INVALID_STATUS")), "search with invalid status"),
                Arguments.of(41, HttpStatus.SC_OK, Map.of("deviceTypes", List.of(999)), "search with non-existent type ID"),
                Arguments.of(42, HttpStatus.SC_OK, Map.of("deviceBrands", List.of(999)), "search with non-existent brand ID"),

                // Empty/null filter values
                Arguments.of(43, HttpStatus.SC_OK, Map.of("customerName", ""), "search with empty customer name"),
                Arguments.of(44, HttpStatus.SC_OK, Map.of("customerCpf", ""), "search with empty CPF"),
                Arguments.of(45, HttpStatus.SC_OK, Map.of("customerPhone", ""), "search with empty phone"),
                Arguments.of(46, HttpStatus.SC_OK, Map.of("status", List.of()), "search with empty status list"),
                Arguments.of(47, HttpStatus.SC_OK, Map.of("deviceTypes", List.of()), "search with empty type IDs list"),
                Arguments.of(48, HttpStatus.SC_OK, Map.of("deviceBrands", List.of()), "search with empty brand IDs list"),

                // Date edge cases
                Arguments.of(49, HttpStatus.SC_OK, Map.of("initialEntryDate", "2030-01-01"), "search with future initial date"),
                Arguments.of(50, HttpStatus.SC_OK, Map.of("finalEntryDate", "2000-01-01"), "search with past final date"),
                Arguments.of(51, HttpStatus.SC_OK, Map.of(
                        "initialEntryDate", "2025-01-01",
                        "finalEntryDate", "2024-12-31"
                ), "search with invalid date range (initial > final)"),

                // Validation tests (invalid parameters)
                Arguments.of(52, HttpStatus.SC_BAD_REQUEST, Map.of("page", -1), "pagination with negative page"),
                Arguments.of(53, HttpStatus.SC_BAD_REQUEST, Map.of("size", 0), "pagination with zero size"),
                Arguments.of(54, HttpStatus.SC_BAD_REQUEST, Map.of("size", -1), "pagination with negative size"),

                // Large page size edge case
                Arguments.of(55, HttpStatus.SC_OK, Map.of("page", 0, "size", 100), "pagination with very large page size")
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

    @Order(4)
    @DisplayName("Update device")
    @MethodSource("updateDeviceArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void updateDevice(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .body(params)
                .put(BASE_PATH + "/update")
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        String actualStatus = params.getOrDefault("deviceStatus", "NOVO").toString();
        boolean isResponseOK = response.statusCode() == 200;
        boolean isStatusDeliveredOrDiscarded = List.of("ENTREGUE", "DESCARTADO").contains(actualStatus);

        if (isResponseOK && isStatusDeliveredOrDiscarded) {
            LocalDate actualDate = LocalDate.now();
            String departureDateString = response.jsonPath().get("departureDate").toString();
            LocalDate departureDate = LocalDate.parse(departureDateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            Assertions.assertThat(departureDate)
                    .as("A propriedade 'departureDate' deve ser diferente de nula quando o status está entre 'ENTREGUE' ou 'DESCARTADO'")
                    .isNotNull().satisfies(date -> Assertions.assertThat(date).isEqualTo(actualDate));
        }

        super.validateResponseIgnoreAttributes(index, response, List.of("lastUpdate", "departureDate"));
    }

    private static Stream<Arguments> updateDeviceArguments() {
        return Stream.of(
                // Successful update tests
                Arguments.of(1, HttpStatus.SC_OK, new HashMap<String, Object>() {{
                    put("deviceId", 1);
                    put("deviceStatus", "EM_ANDAMENTO");
                    put("problem", "Problema atualizado");
                    put("observation", "Observação atualizada");
                    put("budget", "Orçamento atualizado");
                    put("laborValue", 75.0);
                    put("serviceValue", 150.0);
                    put("laborValueCollected", true);
                    put("hasUrgency", false);
                    put("revision", true);
                    put("technicianId", 2);
                }}, "update device with all fields"),

                Arguments.of(2, HttpStatus.SC_OK, new HashMap<String, Object>() {{
                    put("deviceId", 2);
                    put("deviceStatus", "AGUARDANDO");
                    put("problem", "Problema simples");
                    put("observation", "Observação simples");
                    put("budget", "Orçamento simples");
                    put("laborValue", 50.0);
                    put("serviceValue", 100.0);
                    put("laborValueCollected", false);
                    put("hasUrgency", true);
                    put("revision", false);
                }}, "update device without technicianId"),

                Arguments.of(3, HttpStatus.SC_OK, new HashMap<String, Object>() {{
                    put("deviceId", 3);
                    put("deviceStatus", "ENTREGUE");
                    put("problem", "Problema resolvido");
                    put("observation", "Aparelho entregue");
                    put("budget", "Orçamento final");
                    put("laborValue", 100.0);
                    put("serviceValue", 200.0);
                    put("laborValueCollected", true);
                    put("hasUrgency", false);
                    put("revision", false);
                    put("technicianId", 1);
                }}, "update device to status ENTREGUE"),

                Arguments.of(4, HttpStatus.SC_OK, Map.of(
                        "deviceId", 4,
                        "deviceStatus", "DESCARTADO",
                        "problem", "Aparelho descartado",
                        "observation", "Não foi possível consertar",
                        "budget", "Orçamento descartado",
                        "laborValue", 50.0,
                        "serviceValue", 100.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update device to status DESCARTADO"),

                Arguments.of(5, HttpStatus.SC_OK, new HashMap<String, Object>() {{
                    put("deviceId", 5);
                    put("deviceStatus", "PRONTO");
                    put("problem", "Aparelho pronto");
                    put("observation", "Aguardando retirada");
                    put("budget", "Orçamento final");
                    put("laborValue", 80.0);
                    put("serviceValue", 160.0);
                    put("laborValueCollected", false);
                    put("hasUrgency", false);
                    put("revision", true);
                    put("technicianId", 2);
                }}, "update device to status PRONTO"),

                // Validation tests - required fields
                Arguments.of(6, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceStatus", "EM_ANDAMENTO",
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 50.0,
                        "serviceValue", 100.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update device without deviceId"),

                Arguments.of(7, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 50.0,
                        "serviceValue", 100.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update device without deviceStatus"),

                Arguments.of(8, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "deviceStatus", "EM_ANDAMENTO",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 50.0,
                        "serviceValue", 100.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update device without problem"),

                // Device not found tests
                Arguments.of(9, HttpStatus.SC_NOT_FOUND, Map.of(
                        "deviceId", 9999,
                        "deviceStatus", "EM_ANDAMENTO",
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 50.0,
                        "serviceValue", 100.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update non-existent device"),

                // Technician not found tests
                Arguments.of(10, HttpStatus.SC_NOT_FOUND, new HashMap<String, Object>() {{
                    put("deviceId", 1);
                    put("deviceStatus", "EM_ANDAMENTO");
                    put("problem", "Problema teste");
                    put("observation", "Observação teste");
                    put("budget", "Orçamento teste");
                    put("laborValue", 50.0);
                    put("serviceValue", 100.0);
                    put("laborValueCollected", false);
                    put("hasUrgency", false);
                    put("revision", false);
                    put("technicianId", 9999);
                }}, "update device with non-existent technician"),

                // Invalid status tests
                Arguments.of(11, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "deviceStatus", "STATUS_INVALIDO",
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 50.0,
                        "serviceValue", 100.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update device with invalid status"),

                // Negative value tests
                Arguments.of(12, HttpStatus.SC_OK, Map.of(
                        "deviceId", 1,
                        "deviceStatus", "EM_ANDAMENTO",
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 0.0,
                        "serviceValue", 0.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update device with zero values"),

                Arguments.of(13, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "deviceStatus", "EM_ANDAMENTO",
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", -10.0,
                        "serviceValue", 20.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update laborValue with negative value"),

                Arguments.of(14, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "deviceId", 1,
                        "deviceStatus", "EM_ANDAMENTO",
                        "problem", "Problema teste",
                        "observation", "Observação teste",
                        "budget", "Orçamento teste",
                        "laborValue", 10.0,
                        "serviceValue", -20.0,
                        "laborValueCollected", false,
                        "hasUrgency", false,
                        "revision", false
                ), "update serviceValue with negative value")
        );
    }
}
