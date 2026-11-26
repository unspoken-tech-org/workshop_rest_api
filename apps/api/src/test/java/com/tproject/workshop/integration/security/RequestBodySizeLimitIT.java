package com.tproject.workshop.integration.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * Bodies above {@code app.request.max-body-size} (1MB) get 413 with the RFC 9457 {@code
 * ProblemDetail} contract, whether the size is declared up front (Content-Length) or only known
 * while reading (Transfer-Encoding: chunked).
 */
@Sql({"/test-scripts/AuthSetup.sql", "/test-scripts/resetTablesSequence.sql"})
class RequestBodySizeLimitIT extends AbstractIntegrationTest {

    private static final int ONE_MB = 1024 * 1024;

    private final HttpClient client =
            HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldReturn413_whenContentLengthExceedsLimit() throws Exception {
        HttpResponse<String> response =
                send(HttpRequest.BodyPublishers.ofByteArray(tokenBody(ONE_MB + 64)));

        assertPayloadTooLarge(response);
    }

    @Test
    void shouldReturn413_whenChunkedBodyExceedsLimit() throws Exception {
        byte[] body = tokenBody(ONE_MB + 64);
        // ofInputStream has no known length, so HTTP/1.1 sends it chunked.
        HttpResponse<String> response =
                send(
                        HttpRequest.BodyPublishers.ofInputStream(
                                () -> new ByteArrayInputStream(body)));

        assertPayloadTooLarge(response);
    }

    @Test
    void shouldReadChunkedBodyBelowLimitNormally() throws Exception {
        byte[] body = tokenBody(ONE_MB - 1024);
        HttpResponse<String> response =
                send(
                        HttpRequest.BodyPublishers.ofInputStream(
                                () -> new ByteArrayInputStream(body)));

        // Read in full and validated: boundDeviceId is over @Size(max = 100).
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(mapper.readTree(response.body()).get("code").asText())
                .isEqualTo("erro.validacao");
    }

    private HttpResponse<String> send(HttpRequest.BodyPublisher publisher) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(uri("/api/auth/token"))
                        .timeout(Duration.ofSeconds(30))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", API_KEY)
                        .POST(publisher)
                        .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void assertPayloadTooLarge(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        JsonNode body = mapper.readTree(response.body());
        assertThat(body.get("status").asInt()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(body.get("title").asText()).isEqualTo("Payload muito grande");
        assertThat(body.get("code").asText()).isEqualTo("payload.too.large");
        assertThat(body.get("detail").asText()).contains("1MB");
    }

    /** A token request whose total size is {@code totalBytes}, padded in boundDeviceId. */
    private static byte[] tokenBody(int totalBytes) {
        String prefix = "{\"boundDeviceId\":\"";
        String suffix = "\",\"appVersion\":\"1.0.0\"}";
        String padding = "d".repeat(totalBytes - prefix.length() - suffix.length());
        return (prefix + padding + suffix).getBytes(StandardCharsets.UTF_8);
    }
}
