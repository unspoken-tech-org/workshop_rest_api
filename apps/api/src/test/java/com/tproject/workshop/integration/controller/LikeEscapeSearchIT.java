package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.jdbc.Sql;

/**
 * User input in LIKE/ILIKE filters is matched literally: {@code %} and {@code _} are not wildcards
 * and {@code \} never breaks the pattern (no 500).
 */
@Sql({
    "/test-scripts/AuthSetup.sql",
    "/test-scripts/LikeEscapeSearchIT.script.sql",
    "/test-scripts/resetTablesSequence.sql"
})
public class LikeEscapeSearchIT extends AbstractIntegrationTest {

    private static final String LOJA_100 = "Loja 100% Eletrica";
    private static final String LOJA_1000 = "Loja 1000 Eletrica";
    private static final String OFICINA_A_B = "Oficina a_b";
    private static final String BARRA = "Barra \\ Invertida";
    private static final String JOSE = "José Ávila";

    @ParameterizedTest(name = "device filter customerName \"{0}\" -> {1}")
    @MethodSource("deviceFilterByCustomerName")
    void deviceFilterMatchesCustomerNameLiterally(String customerName, List<String> expected) {
        List<String> names =
                post("/v1/device/filter", Map.of("customerName", customerName))
                        .getList("content.customerName", String.class);

        assertThat(names).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Stream<Arguments> deviceFilterByCustomerName() {
        return Stream.of(
                // Matched as literal characters, not as "anything".
                Arguments.of("%", List.of(LOJA_100)),
                Arguments.of("_", List.of(OFICINA_A_B)),
                Arguments.of("\\", List.of(BARRA)),
                Arguments.of("100%", List.of(LOJA_100)),
                Arguments.of("a_b", List.of(OFICINA_A_B)),
                Arguments.of("ending\\", List.of()),
                // Plain and accent-insensitive searches keep working.
                Arguments.of("loja", List.of(LOJA_100, LOJA_1000)),
                Arguments.of("avila", List.of(JOSE)));
    }

    @ParameterizedTest(name = "phone filter \"{0}\" matches nothing")
    @ValueSource(strings = {"%", "_", "\\", "%%", "4491111000_"})
    void phoneFiltersDoNotTreatInputAsWildcard(String phone) {
        assertThat(post("/v1/device/filter", Map.of("customerPhone", phone)).getList("content"))
                .isEmpty();
        assertThat(post("/v1/device/search", Map.of("customerPhone", phone)).getList("content"))
                .isEmpty();
        assertThat(post("/v1/customer/search", Map.of("phone", phone)).getList("content"))
                .isEmpty();
    }

    @ParameterizedTest(name = "phone filter partial match still works")
    @ValueSource(strings = {"10005"})
    void phoneFiltersStillMatchPartialDigits(String phone) {
        assertThat(
                        post("/v1/device/filter", Map.of("customerPhone", phone))
                                .getList("content.customerName", String.class))
                .containsExactly(BARRA);
        assertThat(
                        post("/v1/device/search", Map.of("customerPhone", phone))
                                .getList("content.customerName", String.class))
                .containsExactly(BARRA);
        assertThat(
                        post("/v1/customer/search", Map.of("phone", phone))
                                .getList("content.name", String.class))
                .containsExactly(BARRA);
    }

    @ParameterizedTest(name = "fuzzy search with \"{0}\" returns 200")
    @ValueSource(strings = {"%", "_", "\\", "100%", "a_b", "ending\\", "%_\\"})
    void fuzzySearchesAcceptLikeMetacharacters(String query) {
        post("/v1/device/search", Map.of("searchQuery", query));
        post("/v1/customer/search", Map.of("searchName", query, "searchEmail", query));
        post("/v1/brand/search", Map.of("query", query));
        post("/v1/model/search", Map.of("query", query, "typeId", 1, "brandId", 1));
        post("/v1/type/search", Map.of("query", query));
    }

    @ParameterizedTest(name = "prefix boost of \"{0}\" is literal")
    @ValueSource(strings = {"turbo 100%"})
    void devicePrefixBoostTreatsPercentLiterally(String query) {
        // Only "Turbo 100%" is a literal prefix match; "Turbo 1000" must rank after it.
        List<String> models =
                post("/v1/device/search", Map.of("searchQuery", query))
                        .getList("content.model", String.class);

        assertThat(models).hasSize(6);
        assertThat(models.subList(0, 3)).containsOnly("Turbo 100%");
        assertThat(models.subList(3, 6)).containsOnly("Turbo 1000");
    }

    @ParameterizedTest(name = "accent-insensitive fuzzy search \"{0}\"")
    @ValueSource(strings = {"britania"})
    void fuzzySearchStillIgnoresAccents(String query) {
        assertThat(post("/v1/brand/search", Map.of("query", query)).getList("content.brand"))
                .containsExactly("Britânia");
    }

    private io.restassured.path.json.JsonPath post(String path, Map<String, Object> body) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .when()
                .post(path)
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .jsonPath();
    }
}
