package com.tproject.workshop.config.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class RequestResponseLoggingFilterTest {

    @Test
    void shouldLogRouteWithoutConcreteUriQueryStringOrBody() throws Exception {
        Logger accessLogger = (Logger) LoggerFactory.getLogger("http.access");
        Level previousLevel = accessLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);
        accessLogger.setLevel(Level.INFO);

        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/customers/31781477051");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/customers/{id}");
            request.setQueryString("email=customer%40example.com&token=secret");
            request.setContent("{\"email\":\"customer@example.com\",\"password\":\"secret\"}".getBytes());
            MockHttpServletResponse response = new MockHttpServletResponse();

            new RequestResponseLoggingFilter().doFilter(request, response, (servletRequest, servletResponse) ->
                    ((MockHttpServletResponse) servletResponse).setStatus(400));

            assertThat(appender.list).hasSize(1);
            String message = appender.list.get(0).getFormattedMessage();
            assertThat(message).contains("POST", "/v1/customers/{id}", "400");
            assertThat(message).doesNotContain("31781477051", "email=", "token=", "requestBody", "password", "secret");
        } finally {
            accessLogger.detachAppender(appender);
            accessLogger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @Test
    void shouldNotLogPathWhenRouteMatched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/customers/31781477051");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/customers/{id}");

        ILoggingEvent event = logAccess(request, 200);

        assertThat(event.getMDCPropertyMap()).doesNotContainKey("path");
    }

    @Test
    void shouldLogRequestedPathWithoutQueryStringWhenNoRouteMatched() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/login");
        request.setQueryString("token=secret");

        ILoggingEvent event = logAccess(request, 401);

        assertThat(event.getMDCPropertyMap()).containsEntry("path", "/admin/login");
        assertThat(event.getMDCPropertyMap().get("path")).doesNotContain("token", "secret", "?");
    }

    private static ILoggingEvent logAccess(MockHttpServletRequest request, int status) throws Exception {
        Logger accessLogger = (Logger) LoggerFactory.getLogger("http.access");
        Level previousLevel = accessLogger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        accessLogger.addAppender(appender);
        accessLogger.setLevel(Level.INFO);

        try {
            new RequestResponseLoggingFilter().doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) ->
                    ((MockHttpServletResponse) servletResponse).setStatus(status));

            assertThat(appender.list).hasSize(1);
            return appender.list.get(0);
        } finally {
            accessLogger.detachAppender(appender);
            accessLogger.setLevel(previousLevel);
            appender.stop();
        }
    }
}
