package com.tproject.workshop.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class IdempotencyConfig {

    /** Paths (POST) that honour {@code Idempotency-Key}. Add new endpoints here. */
    static final String[] IDEMPOTENT_PATHS = {"/v1/payment"};

    /** After the Spring Security chain (order -100), so the principal is already set. */
    private static final int ORDER_AFTER_SECURITY = -50;

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(
            IdempotencyService service, ObjectMapper objectMapper) {
        var registration =
                new FilterRegistrationBean<>(new IdempotencyFilter(service, objectMapper));
        registration.addUrlPatterns(IDEMPOTENT_PATHS);
        registration.setOrder(ORDER_AFTER_SECURITY);
        return registration;
    }
}
