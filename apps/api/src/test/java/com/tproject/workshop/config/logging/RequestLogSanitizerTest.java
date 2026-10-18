package com.tproject.workshop.config.logging;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogSanitizerTest {

    @Test
    void shouldDescribeRequestByRouteWithoutConcreteValues() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/customers/31781477051");
        request.setQueryString("email=customer%40example.com&token=secret");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/customers/{id}");

        String description = RequestLogSanitizer.route(new ServletWebRequest(request));

        assertThat(description).isEqualTo("route=/v1/customers/{id}");
        assertThat(description).doesNotContain("31781477051", "email", "token", "secret");
    }

    @Test
    void shouldNotFallBackToConcreteUriWhenNoRouteMatched() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/customers/31781477051");

        assertThat(RequestLogSanitizer.route(new ServletWebRequest(request))).isEqualTo("route=unmatched");
        assertThat(RequestLogSanitizer.route(request)).isEqualTo("unmatched");
    }

    @Test
    void shouldExposeRequestedPathWithoutQueryStringWhenNoRouteMatched() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/wp-admin/setup.php");
        request.setQueryString("token=secret");

        assertThat(RequestLogSanitizer.unmatchedPath(request)).contains("/wp-admin/setup.php");
    }

    @Test
    void shouldNotExposePathWhenRouteMatched() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/customers/31781477051");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/customers/{id}");

        assertThat(RequestLogSanitizer.unmatchedPath(request)).isEmpty();
    }

    @Test
    void shouldTruncateUnmatchedPathToMaxLength() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/" + "a".repeat(300));

        assertThat(RequestLogSanitizer.unmatchedPath(request).orElseThrow())
                .hasSize(RequestLogSanitizer.MAX_UNMATCHED_PATH_LENGTH)
                .startsWith("/aaa");
    }

    @Test
    void shouldReplaceControlCharactersInUnmatchedPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/probe\r\nINFO forged=true");

        assertThat(RequestLogSanitizer.unmatchedPath(request)).contains("/probe__INFO forged=true");
    }
}
