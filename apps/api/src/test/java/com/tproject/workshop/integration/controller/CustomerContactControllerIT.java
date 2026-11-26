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

class CustomerContactControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/customer-contact";
    private static final String CONTACT_PHONE = "44987654321";
    private static final String CONTACT_DATE = "2025-06-03T23:08:14.110245";

    @Autowired private ApplicationContext context;

    private int deviceId;
    private int technicianId;
    private int existingContactId;

    @BeforeEach
    void seedDevice() {
        IntegrationFixtures fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        int customer = fixtures.customer("Test Customer CContact");
        fixtures.customerPhone(customer, CONTACT_PHONE, true, "Telefone Teste");
        int defaultTechnician = fixtures.technician("Default Tech", "T001");
        technicianId = fixtures.technician("Contact Test Tech", "T002");
        deviceId =
                fixtures.device(
                        customer, fixtures.anyBrandModelType(), defaultTechnician, "EM_ANDAMENTO");
        existingContactId =
                fixtures.customerContact(
                        deviceId, defaultTechnician, "2024-01-10 09:00:00", "EM_ANDAMENTO");
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

    // -------------------------------------------------------------------- save

    @Test
    @DisplayName("Saves a contact and returns it")
    void shouldSaveContact_whenRequestIsValid() {
        JsonPath body = save(validBody(), HttpStatus.CREATED);

        assertThat(body.getInt("id")).isNotEqualTo(existingContactId).isPositive();
        assertThat(body.getInt("deviceId")).isEqualTo(deviceId);
        assertThat(body.getInt("technicianId")).isEqualTo(technicianId);
        assertThat(body.getString("technicianName")).isEqualTo("Contact Test Tech");
        assertThat(body.getString("phone")).isEqualTo(CONTACT_PHONE);
        assertThat(body.getString("type")).isEqualTo("mensagem");
        assertThat(body.getBoolean("hasMadeContact")).isTrue();
        assertThat(body.getString("conversation")).isEqualTo("teste");
        assertThat(body.getString("deviceStatus")).isEqualTo("ENTREGUE");
        assertThat(body.getString("lastContact")).isEqualTo(CONTACT_DATE);
        assertThat(countContacts()).isEqualTo(2);
    }

    @Test
    @DisplayName("Accepts a phone number with formatting characters")
    void shouldNormalizePhone_whenNumberHasSpecialCharacters() {
        Map<String, Object> request = validBody();
        request.put("phoneNumber", "(44) 98765-4321");

        JsonPath body = save(request, HttpStatus.CREATED);

        assertThat(body.getString("phone")).isEqualTo(CONTACT_PHONE);
    }

    @ParameterizedTest(name = "missing {0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "deviceId|O ID do dispositivo é obrigatório",
                "technicianId|O ID do técnico é obrigatório",
                "message|A mensagem é obrigatória",
                "contactStatus|O status do contato é obrigatório",
                "deviceStatus|O status do dispositivo é obrigatório",
                "contactDate|A data do contato é obrigatória"
            })
    @DisplayName("Rejects a contact missing a required field")
    void shouldReturn400_whenRequiredFieldIsMissing(String field, String detail) {
        Map<String, Object> request = validBody();
        request.remove(field);

        JsonPath body = save(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail")).isEqualTo(detail);
        assertThat(countContacts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects an empty message")
    void shouldReturn400_whenMessageIsEmpty() {
        Map<String, Object> request = validBody();
        request.put("message", "");

        JsonPath body = save(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail")).isEqualTo("A mensagem é obrigatória");
    }

    @Test
    @DisplayName("Rejects a message contact without phone number")
    void shouldReturn400_whenPhoneIsMissingForMessage() {
        Map<String, Object> request = validBody();
        request.remove("phoneNumber");

        JsonPath body = save(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("requisicao.invalida");
        assertThat(body.getString("detail"))
                .isEqualTo(
                        "O número de telefone deve ser fornecido para os tipos de contato: ligacao, mensagem");
    }

    @Test
    @DisplayName("Rejects a phone number shorter than 10 digits")
    void shouldReturn400_whenPhoneIsTooShort() {
        Map<String, Object> request = validBody();
        request.put("phoneNumber", "9999");

        JsonPath body = save(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo(
                        "O número de telefone, se preenchido, deve ter no mínimo 10 caracteres e no máximo 11");
    }

    @Test
    @DisplayName("Rejects an unknown device status")
    void shouldReturn400_whenDeviceStatusIsUnknown() {
        Map<String, Object> request = validBody();
        request.put("deviceStatus", "non_existent_status");

        JsonPath body = save(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo("Não foi encontrado o status: non_existent_status");
    }

    @Test
    @DisplayName("Returns 404 for an unknown device")
    void shouldReturn404_whenDeviceDoesNotExist() {
        Map<String, Object> request = validBody();
        request.put("deviceId", deviceId + 999);

        JsonPath body = save(request, HttpStatus.NOT_FOUND);

        assertThat(body.getString("code")).isEqualTo("entity.not.found.for.request");
        assertThat(body.getString("detail"))
                .isEqualTo("Aparelho com id " + (deviceId + 999) + " não encontrado");
        assertThat(countContacts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Returns 404 for an unknown technician")
    void shouldReturn404_whenTechnicianDoesNotExist() {
        Map<String, Object> request = validBody();
        request.put("technicianId", technicianId + 999);

        JsonPath body = save(request, HttpStatus.NOT_FOUND);

        assertThat(body.getString("detail"))
                .isEqualTo("Técnico com id " + (technicianId + 999) + " não encontrado");
    }

    @Test
    @DisplayName("Returns 404 for a phone number that is not registered")
    void shouldReturn404_whenPhoneIsNotRegistered() {
        Map<String, Object> request = validBody();
        request.put("phoneNumber", "99999999999");

        JsonPath body = save(request, HttpStatus.NOT_FOUND);

        assertThat(body.getString("detail")).isEqualTo("O número informado não está cadastrado");
    }

    // ------------------------------------------------------------------ update

    @Test
    @DisplayName("Updates every field of a contact")
    void shouldUpdateContact_whenRequestIsValid() {
        Map<String, Object> request = validBody();
        request.put("message", "updated message");

        JsonPath body = update(existingContactId, request, HttpStatus.OK);

        assertThat(body.getInt("id")).isEqualTo(existingContactId);
        assertThat(body.getInt("technicianId")).isEqualTo(technicianId);
        assertThat(body.getString("technicianName")).isEqualTo("Contact Test Tech");
        assertThat(body.getString("conversation")).isEqualTo("updated message");
        assertThat(body.getString("deviceStatus")).isEqualTo("ENTREGUE");
        assertThat(countContacts()).isEqualTo(1);
    }

    @Test
    @DisplayName("Updates the contact type and status")
    void shouldUpdateTypeAndStatus_whenUpdating() {
        Map<String, Object> request = validBody();
        request.put("contactType", "WHATSAPP");
        request.put("deviceStatus", "PRONTO");
        request.put("message", "device is ready");

        JsonPath body = update(existingContactId, request, HttpStatus.OK);

        assertThat(body.getString("type")).isEqualTo("whatsapp");
        assertThat(body.getString("deviceStatus")).isEqualTo("PRONTO");
        assertThat(body.getString("conversation")).isEqualTo("device is ready");
    }

    @ParameterizedTest(name = "{0} contact without phone")
    @CsvSource({"mensagem", "ligacao"})
    @DisplayName("Rejects an update without phone for phone-based contact types")
    void shouldReturn400_whenUpdatingWithoutPhone(String type) {
        Map<String, Object> request = validBody();
        request.put("contactType", type);
        request.remove("phoneNumber");

        JsonPath body = update(existingContactId, request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo(
                        "O número de telefone deve ser fornecido para os tipos de contato: ligacao, mensagem");
    }

    @Test
    @DisplayName("Rejects an unknown device status on update")
    void shouldReturn400_whenUpdatingWithUnknownStatus() {
        Map<String, Object> request = validBody();
        request.put("deviceStatus", "non_existent_status");

        JsonPath body = update(existingContactId, request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo("Não foi encontrado o status: non_existent_status");
    }

    @Test
    @DisplayName("Rejects an invalid date on update")
    void shouldReturn400_whenUpdatingWithInvalidDate() {
        Map<String, Object> request = validBody();
        request.put("contactDate", "invalid_date");

        JsonPath body = update(existingContactId, request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo(
                        "Formato de data inválido: 'invalid_date'. Use o padrão ISO 8601 (ex: 2025-08-31T19:55:13)");
    }

    @Test
    @DisplayName("Rejects a short phone number on update")
    void shouldReturn400_whenUpdatingWithShortPhone() {
        Map<String, Object> request = validBody();
        request.put("phoneNumber", "9999");

        JsonPath body = update(existingContactId, request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
    }

    @Test
    @DisplayName("Returns 404 when updating an unknown contact")
    void shouldReturn404_whenContactDoesNotExist() {
        int unknown = existingContactId + 999;

        JsonPath body = update(unknown, validBody(), HttpStatus.NOT_FOUND);

        assertThat(body.getString("detail"))
                .isEqualTo("Contato com id " + unknown + " não encontrado");
    }

    @Test
    @DisplayName("Returns 404 when updating with an unknown device")
    void shouldReturn404_whenUpdatingWithUnknownDevice() {
        Map<String, Object> request = validBody();
        request.put("deviceId", deviceId + 999);

        JsonPath body = update(existingContactId, request, HttpStatus.NOT_FOUND);

        assertThat(body.getString("detail"))
                .isEqualTo("Aparelho com id " + (deviceId + 999) + " não encontrado");
    }

    @Test
    @DisplayName("Returns 404 when updating with an unknown technician")
    void shouldReturn404_whenUpdatingWithUnknownTechnician() {
        Map<String, Object> request = validBody();
        request.put("technicianId", technicianId + 999);

        JsonPath body = update(existingContactId, request, HttpStatus.NOT_FOUND);

        assertThat(body.getString("detail"))
                .isEqualTo("Técnico com id " + (technicianId + 999) + " não encontrado");
    }

    // -------------------------------------------------------- latest contact

    @Test
    @DisplayName("Only the latest contact changes the current device status")
    void shouldOnlyChangeDeviceStatus_whenContactIsTheLatest() {
        // The newest contact (2025) marks the device as ENTREGUE.
        int latestId =
                save(contactBody("entregue", "2025-06-03T10:00:00"), HttpStatus.CREATED)
                        .getInt("id");
        assertDeviceStatus("ENTREGUE");

        // Fixing the old contact (2024) does not revert the device.
        update(
                existingContactId,
                contactBody("em_andamento", "2024-01-10T09:00:00"),
                HttpStatus.OK);
        assertDeviceStatus("ENTREGUE");

        // A retroactive contact does not change the current status either.
        save(contactBody("aguardando", "2023-01-01T09:00:00"), HttpStatus.CREATED);
        assertDeviceStatus("ENTREGUE");

        // Changing the status of the latest contact is reflected on the device.
        update(latestId, contactBody("pronto", "2025-06-03T10:00:00"), HttpStatus.OK);
        assertDeviceStatus("PRONTO");
    }

    // ----------------------------------------------------------------- helpers

    private void assertDeviceStatus(String expected) {
        String status =
                jdbcTemplate.queryForObject(
                        "SELECT device_status FROM devices WHERE id = ?", String.class, deviceId);
        assertThat(status).isEqualTo(expected);
    }

    private int countContacts() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customer_contact", Integer.class);
    }

    private JsonPath save(Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private JsonPath update(int id, Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .put(BASE_PATH + "/{id}", id)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("deviceId", deviceId);
        body.put("contactType", "mensagem");
        body.put("technicianId", technicianId);
        body.put("phoneNumber", CONTACT_PHONE);
        body.put("message", "teste");
        body.put("contactStatus", true);
        body.put("deviceStatus", "entregue");
        body.put("contactDate", CONTACT_DATE);
        return body;
    }

    private Map<String, Object> contactBody(String deviceStatus, String contactDate) {
        Map<String, Object> body = validBody();
        body.put("contactType", "pessoalmente");
        body.remove("phoneNumber");
        body.put("deviceStatus", deviceStatus);
        body.put("contactDate", contactDate);
        return body;
    }
}
