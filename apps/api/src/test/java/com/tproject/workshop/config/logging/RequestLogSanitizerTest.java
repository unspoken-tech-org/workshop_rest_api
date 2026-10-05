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
}
