package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.integration.support.TestAuthClient;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class ApiKeyControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/admin/api-keys";
    private static final String AUTH_PATH = "/api/auth/token";
    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired private ApplicationContext context;

    private IntegrationFixtures fixtures;
    private long adminId;
    private long serviceId;

    @BeforeEach
    void seedApiKeys() {
        fixtures = new IntegrationFixtures(jdbcTemplate, context);
        adminId = fixtures.adminApiKey();
        serviceId = fixtures.serviceApiKey();
    }

    @Test
    @DisplayName("Lists the API keys without exposing their value")
    void shouldListApiKeys_whenAuthenticatedAsAdmin() {
        long inactiveId =
                fixtures.apiKey(
                        "sk_mobile_TEST_KEY_INACTIVE_12345678901234567890",
                        "test_inactive",
                        "inactive_user",
                        "MOBILE",
                        "SERVICE",
                        false,
                        null);
        // Authenticating records lastUsedAt on the admin key.
        getAuthenticatedSpec();

        List<Map<String, Object>> keys =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath()
                        .getList("$");

        assertThat(keys).hasSize(3);
        assertThat(keys).allSatisfy(key -> assertThat(key.get("maskedKeyValue")).isEqualTo("****"));
        assertThat(keys).allSatisfy(key -> assertThat(key).doesNotContainKey("keyValue"));
        Map<String, Object> admin = byId(keys, adminId);
        assertThat(admin)
                .containsEntry("clientName", "test_app")
                .containsEntry("userIdentifier", "admin_user")
                .containsEntry("platform", "MOBILE")
                .containsEntry("role", "ADMIN")
                .containsEntry("active", true);
        assertThat(admin.get("lastUsedAt")).isNotNull();
        assertThat(byId(keys, serviceId)).containsEntry("role", "SERVICE");
        assertThat(byId(keys, inactiveId)).containsEntry("active", false);
    }

    @Test
    @DisplayName("Rejects listing without JWT")
    void shouldReturn401_whenListingWithoutAuth() {
        given().spec(SPEC).get(BASE_PATH).then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource({
        "new_client_mobile,tech_1,MOBILE,sk_mobile_",
        "new_client_web,web_user,WEB,sk_web_"
    })
    @DisplayName("Creates an API key and returns the plaintext value once")
    void shouldCreateApiKey_whenRequestIsValid(
            String clientName, String userIdentifier, String platform, String prefix) {
        JsonPath body =
                create(
                        Map.of(
                                "clientName", clientName,
                                "userIdentifier", userIdentifier,
                                "platform", platform,
                                "description", "New client"),
                        HttpStatus.CREATED);

        assertThat(body.getString("keyValue")).startsWith(prefix);
        assertThat(body.getString("clientName")).isEqualTo(clientName);
        assertThat(body.getString("userIdentifier")).isEqualTo(userIdentifier);
        assertThat(body.getString("platform")).isEqualTo(platform);
        assertThat(body.getString("role")).isEqualTo("SERVICE");
        assertThat(body.getString("description")).isEqualTo("New client");
        assertThat(body.getString("createdAt")).isNotBlank();
        assertThat(body.getLong("id")).isNotIn(adminId, serviceId);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT key_value FROM api_keys WHERE id = ?",
                                String.class,
                                body.getLong("id")))
                .isNotEqualTo(body.getString("keyValue"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "clientName|Nome do cliente é obrigatório",
                "platform|Plataforma é obrigatória",
                "userIdentifier|Identificador do usuário é obrigatório"
            })
    @DisplayName("Rejects a request missing a required field")
    void shouldReturn400_whenRequiredFieldIsMissing(String missing, String detail) {
        Map<String, Object> request = new HashMap<>();
        request.put("clientName", "test_client");
        request.put("userIdentifier", "tech_1");
        request.put("platform", "MOBILE");
        request.put("description", "desc");
        request.remove(missing);

        JsonPath body = create(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail")).isEqualTo(detail);
        assertThat(countKeys()).isEqualTo(2);
    }

    @Test
    @DisplayName("Rejects a second active key for the same client, user and platform")
    void shouldReturn409_whenKeyAlreadyExistsForClientUserAndPlatform() {
        JsonPath body =
                create(
                        Map.of(
                                "clientName", "test_app",
                                "userIdentifier", "admin_user",
                                "platform", "MOBILE",
                                "description", "Duplicate - should fail"),
                        HttpStatus.CONFLICT);

        assertThat(body.getString("code")).isEqualTo("recurso.conflito");
        assertThat(countKeys()).isEqualTo(2);
    }

    @Test
    @DisplayName("SERVICE role cannot create API keys")
    void shouldReturn403_whenCreatingWithServiceRole() {
        given().spec(getServiceAuthenticatedSpec())
                .body(
                        Map.of(
                                "clientName", "some_client",
                                "userIdentifier", "some_user",
                                "platform", "MOBILE"))
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        assertThat(countKeys()).isEqualTo(2);
    }

    @Test
    @DisplayName("SERVICE role cannot revoke API keys")
    void shouldReturn403_whenRevokingWithServiceRole() {
        given().spec(getServiceAuthenticatedSpec())
                .delete(BASE_PATH + "/" + adminId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT active FROM api_keys WHERE id = ?", Boolean.class, adminId))
                .isTrue();
    }

    @Test
    @DisplayName("Resetting the device binding lets the key log in from another device")
    void shouldResetDeviceBinding() {
        login(TestAuthClient.SERVICE_API_KEY, "device-a", HttpStatus.OK);
        login(TestAuthClient.SERVICE_API_KEY, "device-b", HttpStatus.CONFLICT);

        given().spec(getAuthenticatedSpec())
                .post(BASE_PATH + "/" + serviceId + "/reset-device")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        login(TestAuthClient.SERVICE_API_KEY, "device-b", HttpStatus.OK);
    }

    private void login(String apiKey, String deviceId, HttpStatus expected) {
        given().spec(SPEC)
                .header(API_KEY_HEADER, apiKey)
                .body(Map.of("boundDeviceId", deviceId, "appVersion", "1.0.0"))
                .post(AUTH_PATH)
                .then()
                .statusCode(expected.value());
    }

    private JsonPath create(Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private int countKeys() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM api_keys", Integer.class);
    }

    private static Map<String, Object> byId(List<Map<String, Object>> keys, long id) {
        return keys.stream()
                .filter(key -> ((Number) key.get("id")).longValue() == id)
                .findFirst()
                .orElseThrow();
    }
}
