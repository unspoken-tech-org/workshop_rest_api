package com.tproject.workshop.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.errorhandling.ErrorCode;
import com.tproject.workshop.errorhandling.GlobalExceptionHandler;
import com.tproject.workshop.errorhandling.ResponseError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;
    private final ObjectMapper objectMapper;

    static final String METRICS_ROLE = "METRICS";

    private static final String[] PUBLIC_PATHS = {"/api/auth/**", "/actuator/health"};

    private static final String[] SPRINGDOC_PATHS = {
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/swagger-resources/**",
        "/webjars/**"
    };

    /**
     * Scrape identity with least privilege: a single technical user (HTTP Basic, bcrypt hash
     * supplied by environment) that can only read Prometheus metrics. Without a configured hash the
     * endpoint stays inaccessible.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain metricsScrapeFilterChain(
            HttpSecurity http,
            @Value("${observability.scrape.username:metrics-scraper}") String username,
            @Value("${observability.scrape.password-hash:}") String passwordHash)
            throws Exception {
        http.securityMatcher("/actuator/prometheus")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (passwordHash.isBlank()) {
            return http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll()).build();
        }

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(new BCryptPasswordEncoder());
        provider.setUserDetailsService(
                new InMemoryUserDetailsManager(
                        User.withUsername(username)
                                .password(passwordHash)
                                .roles(METRICS_ROLE)
                                .build()));

        return http.authenticationManager(new ProviderManager(provider))
                .httpBasic(basic -> {})
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole(METRICS_ROLE))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            // Same default as SpringDoc: the docs paths are public only where it is on.
            @Value("${springdoc.api-docs.enabled:true}") boolean springdocEnabled)
            throws Exception {
        String[] publicPaths =
                springdocEnabled
                        ? Stream.concat(Arrays.stream(PUBLIC_PATHS), Arrays.stream(SPRINGDOC_PATHS))
                                .toArray(String[]::new)
                        : PUBLIC_PATHS;
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions ->
                        exceptions
                                // Return 401 instead of 403 when not authenticated
                                .authenticationEntryPoint((request, response, authException) ->
                                        writeErrorResponse(
                                                response,
                                                ErrorCode.AUTHENTICATION_REQUIRED,
                                                GlobalExceptionHandler
                                                        .AUTHENTICATION_REQUIRED_MESSAGE))
                                // Write 403 directly: the default handler's /error dispatch is
                                // not authenticated and would surface as 401 (R19). Same body as
                                // GlobalExceptionHandler#handleAccessDeniedException.
                                .accessDeniedHandler((request, response, accessDeniedException) ->
                                        writeErrorResponse(
                                                response,
                                                ErrorCode.ACCESS_DENIED,
                                                GlobalExceptionHandler.ACCESS_DENIED_MESSAGE)))
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(publicPaths)
                                .permitAll()
                                // Only health is public; /actuator/prometheus is handled by
                                // the scrape chain above and info reveals build details.
                                .requestMatchers("/actuator/**")
                                .hasRole("ADMIN")
                                .requestMatchers("/v1/admin/**")
                                .hasRole("ADMIN")
                                .requestMatchers("/v1/**")
                                .hasAnyRole("ADMIN", "SERVICE")
                                .anyRequest()
                                .denyAll())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeErrorResponse(
            HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ResponseError.of(errorCode, message));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(corsProperties.getAllowedMethods());
        config.setAllowedHeaders(corsProperties.getAllowedHeaders());
        config.setExposedHeaders(corsProperties.getExposedHeaders());
        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
