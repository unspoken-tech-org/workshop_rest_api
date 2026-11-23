package com.tproject.workshop.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

/** CORS is fail-closed: no origin is allowed unless the profile lists it explicitly. */
class CorsConfigurationTest {

    private static final String PATH = "/api/auth/token";

    @Test
    void shouldDefaultToNoOriginsExplicitHeadersAndNoCredentials() {
        CorsProperties properties = new CorsProperties();

        assertThat(properties.getAllowedOrigins()).isEmpty();
        assertThat(properties.getAllowedHeaders())
                .containsExactly("Authorization", "Content-Type", "X-API-Key", "X-Request-Id");
        assertThat(properties.isAllowCredentials()).isFalse();
    }

    @Test
    void shouldRejectEveryOriginWithDefaultProperties() throws IOException {
        CorsConfiguration config = configFor(new CorsProperties());

        for (String origin : List.of("https://evil.example", "http://localhost:3000")) {
            MockHttpServletResponse response = preflight(config, origin, "X-API-Key");

            assertThat(response.getStatus()).as(origin).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                    .as(origin)
                    .isNull();
            assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                    .as(origin)
                    .isNull();
        }
    }

    @Test
    void shouldAllowListedOriginWithApiKeyHeaderAndWithoutCredentials() throws IOException {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("http://127.0.0.1:8081"));
        CorsConfiguration config = configFor(properties);

        MockHttpServletResponse response =
                preflight(config, "http://127.0.0.1:8081", "X-API-Key, Content-Type");

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://127.0.0.1:8081");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("X-API-Key");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isNull();
    }

    @Test
    void shouldRejectHeaderOutsideTheExplicitList() throws IOException {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("http://127.0.0.1:8081"));

        MockHttpServletResponse response =
                preflight(configFor(properties), "http://127.0.0.1:8081", "X-Custom-Header");

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    private static CorsConfiguration configFor(CorsProperties properties) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", PATH);
        CorsConfiguration config =
                new SecurityConfig(null, properties)
                        .corsConfigurationSource()
                        .getCorsConfiguration(request);
        assertThat(config).isNotNull();
        return config;
    }

    private static MockHttpServletResponse preflight(
            CorsConfiguration config, String origin, String requestHeaders) throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", PATH);
        request.setScheme("https");
        request.setServerName("api.eletroluk.com");
        request.setServerPort(443);
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, requestHeaders);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new DefaultCorsProcessor().processRequest(config, request, response);
        return response;
    }
}
