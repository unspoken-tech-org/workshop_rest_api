package com.tproject.workshop.config.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tproject.workshop.exception.PayloadTooLargeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;

class RequestBodySizeLimitFilterTest {

    private static final int LIMIT = 16;

    private final ObjectMapper objectMapper =
            JsonMapper.builder()
                    .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                    .build();
    private final RequestBodySizeLimitFilter filter =
            new RequestBodySizeLimitFilter(DataSize.ofBytes(LIMIT), objectMapper);

    @Test
    void shouldRejectDeclaredContentLengthAboveLimitWithoutCallingChain() throws Exception {
        MockHttpServletRequest request = request(new byte[LIMIT + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentType()).startsWith("application/problem+json");

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.get("status").asInt()).isEqualTo(413);
        assertThat(body.get("title").asText()).isEqualTo("Payload muito grande");
        assertThat(body.get("code").asText()).isEqualTo("payload.too.large");
        assertThat(body.get("detail").asText()).contains("16 bytes");
    }

    @Test
    void shouldPassBodyAtLimitThrough() throws Exception {
        byte[] content = "x".repeat(LIMIT).getBytes(StandardCharsets.UTF_8);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean fullyRead = new AtomicBoolean();

        filter.doFilter(
                request(content),
                response,
                (req, res) ->
                        fullyRead.set(
                                ((HttpServletRequest) req).getInputStream().readAllBytes().length
                                        == LIMIT));

        assertThat(fullyRead).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldFailWhileReadingBodyOfUnknownLengthAboveLimit() {
        MockHttpServletRequest request = chunkedRequest(new byte[LIMIT + 1]);

        FilterChain readStream =
                (req, res) -> ((HttpServletRequest) req).getInputStream().readAllBytes();

        assertThatThrownBy(
                        () -> filter.doFilter(request, new MockHttpServletResponse(), readStream))
                .isInstanceOf(PayloadTooLargeException.class);
    }

    @Test
    void shouldFailWhileReadingBodyOfUnknownLengthByteByByte() {
        MockHttpServletRequest request = chunkedRequest(new byte[LIMIT + 1]);

        FilterChain readStream =
                (req, res) -> {
                    var in = ((HttpServletRequest) req).getInputStream();
                    while (in.read() != -1) {
                        // drain
                    }
                };

        assertThatThrownBy(
                        () -> filter.doFilter(request, new MockHttpServletResponse(), readStream))
                .isInstanceOf(PayloadTooLargeException.class);
    }

    @Test
    void shouldCountBytesReadThroughReader() {
        MockHttpServletRequest request = chunkedRequest("y".repeat(LIMIT + 1).getBytes());

        FilterChain readReader =
                (req, res) -> {
                    BufferedReader reader = req.getReader();
                    while (reader.read() != -1) {
                        // drain
                    }
                };

        assertThatThrownBy(
                        () -> filter.doFilter(request, new MockHttpServletResponse(), readReader))
                .isInstanceOf(PayloadTooLargeException.class);
    }

    private static MockHttpServletRequest request(byte[] content) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/token");
        request.setContentType("application/json");
        request.setContent(content);
        return request;
    }

    /** Transfer-Encoding: chunked: the container reports no Content-Length. */
    private static MockHttpServletRequest chunkedRequest(byte[] content) {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/auth/token") {
                    @Override
                    public long getContentLengthLong() {
                        return -1;
                    }

                    @Override
                    public int getContentLength() {
                        return -1;
                    }
                };
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent(content);
        return request;
    }
}
