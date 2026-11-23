package com.tproject.workshop.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;

    @Mock private ApiKeyRepository apiKeyRepository;

    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectWhenApiKeyIsInactive() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"));

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                ApiKey.builder()
                                        .id(1L)
                                        .active(false)
                                        .boundDeviceId("device-a")
                                        .platform(Platform.MOBILE)
                                        .role(Role.SERVICE)
                                        .build()));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectWhenApiKeyIsExpired() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"));

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                ApiKey.builder()
                                        .id(1L)
                                        .active(true)
                                        .expiresAt(Instant.now().minusSeconds(10))
                                        .boundDeviceId("device-a")
                                        .platform(Platform.MOBILE)
                                        .role(Role.SERVICE)
                                        .build()));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectWhenBoundDeviceWasResetOrMismatches() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"));

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                ApiKey.builder()
                                        .id(1L)
                                        .active(true)
                                        .boundDeviceId(null)
                                        .platform(Platform.MOBILE)
                                        .role(Role.SERVICE)
                                        .build()));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateWhenApiKeyIsActiveNotExpiredAndDeviceMatches() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"));

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L))
                .thenReturn(
                        Optional.of(
                                ApiKey.builder()
                                        .id(1L)
                                        .active(true)
                                        .expiresAt(Instant.now().plusSeconds(3600))
                                        .boundDeviceId("device-a")
                                        .platform(Platform.MOBILE)
                                        .role(Role.SERVICE)
                                        .build()));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SERVICE");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateWhenTokenVersionMatches() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"), 3L);

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(boundApiKey(3L)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectWhenTokenVersionIsOlderThanApiKeyVersion() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"), 2L);

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(boundApiKey(3L)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectWhenTokenVersionIsNewerThanApiKeyVersion() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"), 4L);

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(boundApiKey(3L)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldAcceptLegacyTokenWithoutTvWhileApiKeyVersionIsZero() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"));

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(boundApiKey(0L)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void shouldRejectLegacyTokenWithoutTvOnceApiKeyVersionWasIncremented() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims = claims(1L, "device-a", List.of("ROLE_SERVICE"));

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(boundApiKey(1L)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectWhenTvClaimIsNotNumeric() throws Exception {
        MockHttpServletRequest request = requestWithBearer("valid.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Claims claims =
                Jwts.claims()
                        .subject("test_app")
                        .add("apiKeyId", 1L)
                        .add("boundDeviceId", "device-a")
                        .add("roles", List.of("ROLE_SERVICE"))
                        .add("tv", "0")
                        .build();

        when(jwtService.validateAndGetClaims("valid.jwt")).thenReturn(claims);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(boundApiKey(0L)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private static ApiKey boundApiKey(long tokenVersion) {
        return ApiKey.builder()
                .id(1L)
                .active(true)
                .boundDeviceId("device-a")
                .platform(Platform.MOBILE)
                .role(Role.SERVICE)
                .tokenVersion(tokenVersion)
                .build();
    }

    private static Claims claims(
            Long apiKeyId, String boundDeviceId, List<String> roles, long tokenVersion) {
        return Jwts.claims()
                .subject("test_app")
                .add("apiKeyId", apiKeyId)
                .add("boundDeviceId", boundDeviceId)
                .add("roles", roles)
                .add("tv", tokenVersion)
                .build();
    }

    private static MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/colors");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private static Claims claims(Long apiKeyId, String boundDeviceId, List<String> roles) {
        return Jwts.claims()
                .subject("test_app")
                .add("apiKeyId", apiKeyId)
                .add("boundDeviceId", boundDeviceId)
                .add("roles", roles)
                .build();
    }
}
