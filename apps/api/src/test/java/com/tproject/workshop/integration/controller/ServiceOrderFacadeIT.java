package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

/**
 * Feature 24 phase 2 rules through the legacy {@code /v1/device/*} facade (section 4.3 of
 * PLANO_SEPARACAO_APARELHO_OS.md): revision linked to its origin order and revision kept on close.
 */
class ServiceOrderFacadeIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/device";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;
    private int customerId;
    private int technicianId;
    private int brandModelTypeId;

    @BeforeEach
    void seed() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        customerId = fixtures.customer("Facade Customer");
        technicianId = fixtures.technician("Facade Tech", "T010");
        brandModelTypeId = fixtures.anyBrandModelType();
    }

    @Test
    @DisplayName("Create with originOrderId reuses the physical device and opens a revision")
    void shouldLinkRevisionToOriginOrder() {
        int origin = fixtures.device(customerId, brandModelTypeId, technicianId, "ENTREGUE");

        int created = create(body(customerId, origin, true), HttpStatus.CREATED).getInt("deviceId");

        Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "SELECT id_customer_device, id_origin_order, is_revision, device_status"
                                + " FROM devices WHERE id = ?",
                        created);
        assertThat(row.get("id_customer_device")).isEqualTo(customerDeviceOf(origin));
        assertThat(row.get("id_origin_order")).isEqualTo(origin);
        assertThat(row.get("is_revision")).isEqualTo(true);
        assertThat(row.get("device_status")).isEqualTo("NOVO");
    }

    @Test
    @DisplayName("Create rejects an origin order of another customer with 400")
    void shouldRejectOriginOfAnotherCustomer() {
        int otherCustomer =
                fixtures.customer(
                        "Other Facade Customer", "52998224725", "feminino", "other@example.com");
        int origin = fixtures.device(otherCustomer, brandModelTypeId, technicianId, "ENTREGUE");

        create(body(customerId, origin, true), HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Create rejects a DESCARTADO origin order with 409 customer-device.disposed")
    void shouldRejectDisposedOrigin() {
        int origin = fixtures.device(customerId, brandModelTypeId, technicianId, "DESCARTADO");

        JsonPath body = create(body(customerId, origin, false), HttpStatus.CONFLICT);

        assertThat(body.getString("code")).isEqualTo("customer-device.disposed");
    }

    @Test
    @DisplayName("Create rejects a delivered origin when a later order discarded the device")
    void shouldRejectOriginOfDeviceDiscardedLater() {
        int delivered = fixtures.device(customerId, brandModelTypeId, technicianId, "ENTREGUE");
        int discarded = fixtures.device(customerId, brandModelTypeId, technicianId, "DESCARTADO");
        jdbcTemplate.update(
                "UPDATE devices SET id_customer_device = ? WHERE id = ?",
                customerDeviceOf(delivered),
                discarded);

        JsonPath body = create(body(customerId, delivered, true), HttpStatus.CONFLICT);

        assertThat(body.getString("code")).isEqualTo("customer-device.disposed");
    }

    @Test
    @DisplayName("Closing keeps the revision flag and resets only the urgency")
    void shouldKeepRevisionWhenClosing() {
        int order = fixtures.device(customerId, brandModelTypeId, technicianId, "PRONTO");
        jdbcTemplate.update(
                "UPDATE devices SET is_revision = true, has_urgency = true WHERE id = ?", order);

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .body(Map.of("deviceStatus", "ENTREGUE"))
                        .put(BASE_PATH + "/update/" + order + "/status")
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getString("deviceStatus")).isEqualTo("ENTREGUE");
        assertThat(body.getBoolean("revision")).isTrue();
        assertThat(body.getBoolean("hasUrgency")).isFalse();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT closed_at IS NOT NULL FROM devices WHERE id = ?",
                                Boolean.class,
                                order))
                .isTrue();
    }

    private Integer customerDeviceOf(int orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT id_customer_device FROM devices WHERE id = ?", Integer.class, orderId);
    }

    private JsonPath create(Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .post(BASE_PATH + "/create")
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private Map<String, Object> body(int customer, int originOrderId, boolean revision) {
        Map<String, Object> body = new HashMap<>();
        body.put("customerId", customer);
        body.put(
                "typeBrandModel",
                Map.of("type", "Liquidificador", "brand", "Arno", "model", "Power"));
        body.put("problem", "Mesmo defeito");
        body.put("colors", List.of("preto"));
        body.put("hasUrgency", false);
        body.put("originOrderId", originOrderId);
        body.put("revision", revision);
        return body;
    }
}
