package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class PaymentControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/payment";

    @Autowired private ApplicationContext context;

    private int deviceId;

    @BeforeEach
    void seedDevice() {
        IntegrationFixtures fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        int customer = fixtures.customer("Alfonso Zimmer");
        int technician = fixtures.technician("Technician 1", "44988254493");
        deviceId =
                fixtures.device(customer, fixtures.anyBrandModelType(), technician, "EM_ANDAMENTO");
    }

    @Test
    @DisplayName("Rejects access without JWT")
    void shouldReturn401_whenNoJwt() {
        given().spec(SPEC)
                .body(validBody())
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Creates a payment and persists it")
    void shouldCreatePayment_whenRequestIsValid() {
        JsonPath body = post(validBody(), HttpStatus.CREATED);

        assertThat(body.getInt("id")).isPositive();
        assertThat(body.getInt("deviceId")).isEqualTo(deviceId);
        assertThat(body.getString("paymentType")).isEqualTo("credito");
        assertThat(body.getDouble("paymentValue")).isEqualTo(150.0);
        assertThat(body.getString("category")).isEqualTo("servicos");
        assertThat(body.getString("receivedBy")).isEqualTo("João da Silva");
        assertThat(body.getString("paymentDate")).isNotBlank();

        Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "SELECT id_device, payment_type, payment_value, received_by"
                                + " FROM payments WHERE id = ?",
                        body.getInt("id"));
        assertThat(row.get("id_device")).isEqualTo(deviceId);
        assertThat((String) row.get("payment_type")).isEqualToIgnoringCase("credito");
        assertThat(row.get("received_by")).isEqualTo("João da Silva");
    }

    @Test
    @DisplayName("Keeps the explicit payment date")
    void shouldKeepPaymentDate_whenDateIsInformed() {
        Map<String, Object> request = validBody();
        request.put("paymentDate", "2026-06-01T00:00:00.000");
        request.put("value", 200.00);
        request.put("receivedBy", "Maria Souza");

        JsonPath body = post(request, HttpStatus.CREATED);

        assertThat(body.getString("paymentDate")).isEqualTo("2026-06-01T00:00:00");
        assertThat(body.getDouble("paymentValue")).isEqualTo(200.0);
        assertThat(body.getString("receivedBy")).isEqualTo("Maria Souza");
    }

    @Test
    @DisplayName("Rejects an unknown payment type")
    void shouldReturn400_whenPaymentTypeIsInvalid() {
        Map<String, Object> request = validBody();
        request.put("paymentType", "invalid_type");

        JsonPath body = post(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("requisicao.invalida");
        assertThat(body.getString("detail"))
                .contains("invalid_type")
                .contains("credito, debito, dinheiro, pix, outro");
        assertThat(countPayments()).isZero();
    }

    @Test
    @DisplayName("Rejects a value equal to zero")
    void shouldReturn400_whenValueIsZero() {
        Map<String, Object> request = validBody();
        request.put("value", 0);

        JsonPath body = post(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail"))
                .isEqualTo("Valor do pagamento deve ser maior que zero");
        assertThat(countPayments()).isZero();
    }

    @ParameterizedTest(name = "missing {0}")
    @CsvSource({
        "paymentType,Tipo de pagamento não pode ser nulo",
        "deviceId,ID do dispositivo não pode ser nulo"
    })
    @DisplayName("Rejects a request missing a required field")
    void shouldReturn400_whenRequiredFieldIsMissing(String field, String detail) {
        Map<String, Object> request = validBody();
        request.remove(field);

        JsonPath body = post(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail")).isEqualTo(detail);
        assertThat(countPayments()).isZero();
    }

    @Test
    @DisplayName("Rejects a future payment date")
    void shouldReturn400_whenPaymentDateIsInTheFuture() {
        Map<String, Object> request = validBody();
        request.put("paymentDate", "2099-01-01T00:00:00.000");

        JsonPath body = post(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo("A data do pagamento não pode ser maior que a data atual");
        assertThat(countPayments()).isZero();
    }

    @Test
    @DisplayName("Returns 404 for a device that does not exist")
    void shouldReturn404_whenDeviceDoesNotExist() {
        Map<String, Object> request = validBody();
        request.put("deviceId", deviceId + 9999);

        JsonPath body = post(request, HttpStatus.NOT_FOUND);

        assertThat(body.getString("code")).isEqualTo("entity.not.found.for.request");
        assertThat(body.getString("detail")).contains("não encontrado");
        assertThat(countPayments()).isZero();
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("deviceId", deviceId);
        body.put("paymentType", "credito");
        body.put("value", 150.00);
        body.put("category", "servicos");
        body.put("receivedBy", "João da Silva");
        return body;
    }

    private JsonPath post(Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private int countPayments() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Integer.class);
    }
}
