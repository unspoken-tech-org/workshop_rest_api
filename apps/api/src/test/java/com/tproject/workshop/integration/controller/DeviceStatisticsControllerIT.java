package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class DeviceStatisticsControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/device-statistics";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;
    private int customer;
    private int technician;
    private int brandModelType;

    @BeforeEach
    void seed() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        customer = fixtures.customer("Alfonso Zimmer");
        technician = fixtures.technician("Technician 1", "44988254493");
        brandModelType = fixtures.anyBrandModelType();
    }

    @Test
    @DisplayName("Rejects access without JWT")
    void shouldReturn401_whenNoJwt() {
        given().spec(SPEC).get(BASE_PATH).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Returns zeroed counters when there are no devices")
    void shouldReturnZeroCounters_whenThereAreNoDevices() {
        JsonPath body = statistics();

        for (String status :
                new String[] {
                    "NOVO",
                    "EM_ANDAMENTO",
                    "AGUARDANDO",
                    "ENTREGUE",
                    "PRONTO",
                    "DESCARTADO",
                    "REVISAO",
                    "URGENTE",
                    "APROVADO",
                    "NAO_APROVADO"
                }) {
            assertThat(body.getInt(status + ".total")).as(status).isZero();
            assertThat(body.getInt(status + ".lastMonth")).as(status).isZero();
        }
        assertThat(body.getList("last_viewed_devices")).isEmpty();
    }

    @Test
    @DisplayName("Counts devices per status, and urgency and revision only for open orders")
    void shouldCountDevicesPerStatus_whenDevicesExist() {
        newDevice("EM_ANDAMENTO");
        int urgent = newDevice("EM_ANDAMENTO");
        newDevice("AGUARDANDO");
        newDevice("NOVO");
        int revision = newDevice("NOVO");
        int closedRevision = newDevice("ENTREGUE");
        int closedUrgent = newDevice("DESCARTADO");
        jdbcTemplate.update("UPDATE devices SET has_urgency = true WHERE id = ?", urgent);
        jdbcTemplate.update("UPDATE devices SET is_revision = true WHERE id = ?", revision);
        // Closed orders keep is_revision (decision 7) but are not counted as revision/urgent.
        jdbcTemplate.update("UPDATE devices SET is_revision = true WHERE id = ?", closedRevision);
        jdbcTemplate.update("UPDATE devices SET has_urgency = true WHERE id = ?", closedUrgent);

        JsonPath body = statistics();

        assertThat(body.getInt("EM_ANDAMENTO.total")).isEqualTo(2);
        assertThat(body.getInt("AGUARDANDO.total")).isEqualTo(1);
        assertThat(body.getInt("NOVO.total")).isEqualTo(2);
        assertThat(body.getInt("ENTREGUE.total")).isEqualTo(1);
        assertThat(body.getInt("DESCARTADO.total")).isEqualTo(1);
        assertThat(body.getInt("PRONTO.total")).isZero();
        assertThat(body.getInt("URGENTE.total")).isEqualTo(1);
        assertThat(body.getInt("REVISAO.total")).isEqualTo(1);
    }

    @Test
    @DisplayName("Lists only devices that were viewed, most recent first")
    void shouldListLastViewedDevices_whenDevicesWereViewed() {
        int older = newDevice("NOVO");
        int newer = newDevice("NOVO");
        newDevice("NOVO");
        jdbcTemplate.update(
                "UPDATE devices SET last_viewed_at = now() - interval '2 hours' WHERE id = ?",
                older);
        jdbcTemplate.update("UPDATE devices SET last_viewed_at = now() WHERE id = ?", newer);

        JsonPath body = statistics();

        assertThat(body.getList("last_viewed_devices")).hasSize(2);
        assertThat(body.getList("last_viewed_devices.deviceId", Integer.class))
                .containsExactly(newer, older);
    }

    private int newDevice(String status) {
        return fixtures.device(customer, brandModelType, technician, status);
    }

    private JsonPath statistics() {
        return given().spec(getAuthenticatedSpec())
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath();
    }
}
