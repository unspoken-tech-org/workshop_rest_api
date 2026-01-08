package com.tproject.workshop.integration.controller;

import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import com.tproject.workshop.integration.TestAuthHelper;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
public class ApiKeyControllerIT extends AbstractIntegrationLiveTest {

    private static final String BASE_PATH = "/v1/admin/api-keys";

    @BeforeEach
    void setUp() {
        TestAuthHelper.invalidateTokenCache();
    }

    // ========== List API Keys Tests ==========

    @Order(1)
    @DisplayName("List all API Keys")
    @Test
    public void shouldListAllApiKeys() {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes("list", response, List.of(
                "[0].id", "[0].createdAt", "[0].lastUsedAt", "[0].expiresAt",
                "[1].id", "[1].createdAt", "[1].lastUsedAt", "[1].expiresAt",
                "[2].id", "[2].createdAt", "[2].lastUsedAt", "[2].expiresAt",
                "[3].id", "[3].createdAt", "[3].lastUsedAt", "[3].expiresAt",
                "[4].id", "[4].createdAt", "[4].lastUsedAt", "[4].expiresAt",
                "[5].id", "[5].createdAt", "[5].lastUsedAt", "[5].expiresAt",
                "[6].id", "[6].createdAt", "[6].lastUsedAt", "[6].expiresAt"
        ));
    }

    @Order(2)
    @DisplayName("List API Keys - unauthorized")
    @Test
    public void shouldReturn401_whenListingWithoutAuth() {
        given().spec(SPEC)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    // ========== Create API Key Tests ==========

    @Order(3)
    @DisplayName("Create API Key")
    @MethodSource("createApiKeyArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} reason {3}")
    public void createApiKey(int index, Integer statusCode, Map<String, Object> body, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .body(body)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        if (statusCode == HttpStatus.SC_CREATED) {
            super.validateResponseIgnoreAttributes(index, response, List.of(
                    "id", "keyValue", "createdAt"
            ));
        } else {
            super.validateResponse(index, response);
        }
    }

    private static Stream<Arguments> createApiKeyArguments() {
        return Stream.of(
                // Successful creation
                Arguments.of(1, HttpStatus.SC_CREATED, Map.of(
                        "clientName", "new_client_mobile",
                        "userIdentifier", "tech_1",
                        "platform", "MOBILE",
                        "description", "New mobile client"
                ), "create API Key for new mobile client"),

                Arguments.of(2, HttpStatus.SC_CREATED, Map.of(
                        "clientName", "new_client_web",
                        "userIdentifier", "web_user",
                        "platform", "WEB",
                        "description", "New web client"
                ), "create API Key for new web client"),

                // Validation errors
                Arguments.of(3, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "userIdentifier", "tech_1",
                        "platform", "MOBILE",
                        "description", "Missing client name"
                ), "create API Key without clientName"),

                Arguments.of(4, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "clientName", "test_client",
                        "userIdentifier", "tech_1",
                        "description", "Missing platform"
                ), "create API Key without platform"),

                Arguments.of(6, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "clientName", "test_client",
                        "platform", "MOBILE",
                        "description", "Missing user identifier"
                ), "create API Key without userIdentifier"),

                // Conflict - duplicate (same client, user and platform)
                Arguments.of(5, HttpStatus.SC_UNAUTHORIZED, Map.of(
                        "clientName", "test_app",
                        "userIdentifier", "admin_user",
                        "platform", "MOBILE",
                        "description", "Duplicate - should fail"
                ), "create duplicate API Key - same client, user and platform")
        );
    }

    // ========== RBAC Authorization Tests ==========

    @Order(4)
    @DisplayName("Create API Key - Forbidden (Service Role)")
    @Test
    public void shouldReturn403_whenCreatingWithServiceRole() {
        given().spec(getServiceAuthenticatedSpec())
                .body(Map.of(
                        "clientName", "some_client",
                        "userIdentifier", "some_user",
                        "platform", "MOBILE"
                ))
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Order(5)
    @DisplayName("Revoke API Key - Forbidden (Service Role)")
    @Test
    public void shouldReturn403_whenRevokingWithServiceRole() {
        given().spec(getServiceAuthenticatedSpec())
                .when()
                .delete(BASE_PATH + "/1")
                .then()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}
