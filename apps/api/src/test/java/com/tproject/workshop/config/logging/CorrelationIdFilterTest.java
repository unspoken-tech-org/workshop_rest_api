package com.tproject.workshop.config.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPreserveSafeClientRequestIdAndExposeItInResponse() throws Exception {
        MockHttpServletRequest request = requestWithId("client-request-123_abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainAssertingMdc("client-request-123_abc"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("client-request-123_abc");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateSafeIdForBlankInvalidOrOversizedValues() throws Exception {
        String[] invalidValues = {
                " ",
                "request id with spaces",
                "request/id",
                "request\nforged-log-line",
                "x".repeat(RequestIdValidator.MAX_LENGTH + 1)
        };

        for (String invalidValue : invalidValues) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(requestWithId(invalidValue), response, new NoopFilterChain());

            String generated = response.getHeader(CorrelationIdFilter.HEADER_NAME);
            assertThat(generated).satisfies(value -> {
                assertThat(RequestIdValidator.isValid(value)).isTrue();
                assertThat(UUID.fromString(value)).isNotNull();
            });
        }
    }

    @Test
    void shouldTrimAccidentalOuterWhitespaceWithoutAllowingUnsafeCharacters() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithId("  request-123  "), response, new NoopFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("request-123");
    }

    private MockHttpServletRequest requestWithId(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, value);
        return request;
    }

    private FilterChain chainAssertingMdc(String expectedRequestId) {
        return (request, response) -> assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                .isEqualTo(expectedRequestId);
    }

    private static final class NoopFilterChain implements FilterChain {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest request,
                             jakarta.servlet.ServletResponse response) {
        }
    }
}
