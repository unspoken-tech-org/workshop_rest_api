package com.tproject.workshop.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tproject.workshop.dto.auth.RefreshTokenRequest;
import com.tproject.workshop.dto.auth.TokenRequest;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String API_KEY_VALUE = "sk_mobile_test";

    @Mock private ApiKeyService apiKeyService;

    @Mock private JwtService jwtService;

    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 3600L);
    }

    @Test
    void loginShouldUseTokenVersionReturnedByIncrementAsTvClaim() {
        // The loaded (detached) entity still carries the pre-login version.
        ApiKey apiKey = apiKey(4L);
        when(apiKeyService.findByKeyValue(API_KEY_VALUE)).thenReturn(apiKey);
        when(apiKeyService.incrementTokenVersion(1L)).thenReturn(5L);
        when(jwtService.generateAccessToken(eq("client"), anyMap())).thenReturn("access.jwt");
        when(refreshTokenService.createRefreshToken(apiKey)).thenReturn("rt_new");

        var response =
                authService.authenticate(API_KEY_VALUE, new TokenRequest("device-a", "1.0.0"));

        assertThat(response.accessToken()).isEqualTo("access.jwt");
        Map<String, Object> claims = capturedClaims();
        assertThat(claims).containsEntry("tv", 5L).containsEntry("boundDeviceId", "device-a");

        InOrder order = inOrder(apiKeyService, jwtService);
        order.verify(apiKeyService).bindDeviceOrThrow(apiKey, "device-a");
        order.verify(apiKeyService).incrementTokenVersion(1L);
        order.verify(jwtService).generateAccessToken(eq("client"), anyMap());
    }

    @Test
    void normalRefreshShouldKeepTokenVersionWithoutIncrementing() {
        ApiKey apiKey = apiKey(7L);
        when(refreshTokenService.rotateRefreshToken("rt_old"))
                .thenReturn(new RefreshTokenService.RotationResult(apiKey, "rt_rotated"));
        when(jwtService.generateAccessToken(eq("client"), anyMap())).thenReturn("access.jwt");

        var response = authService.refreshToken(new RefreshTokenRequest("rt_old"));

        assertThat(response.refreshToken()).isEqualTo("rt_rotated");
        assertThat(capturedClaims()).containsEntry("tv", 7L);
        verify(apiKeyService, never()).incrementTokenVersion(anyLong());
    }

    @Test
    void adminLoginWithNullBoundDeviceIdShouldOmitClaimAndIncrementTokenVersion() {
        ApiKey apiKey = adminApiKey(4L);
        when(apiKeyService.findByKeyValue(API_KEY_VALUE)).thenReturn(apiKey);
        when(apiKeyService.incrementTokenVersion(1L)).thenReturn(5L);
        when(jwtService.generateAccessToken(eq("client"), anyMap())).thenReturn("access.jwt");
        when(refreshTokenService.createRefreshToken(apiKey)).thenReturn("rt_new");

        var response = authService.authenticate(API_KEY_VALUE, new TokenRequest(null, "1.0.0"));

        assertThat(response.accessToken()).isEqualTo("access.jwt");
        Map<String, Object> claims = capturedClaims();
        assertThat(claims).containsEntry("tv", 5L).doesNotContainKey("boundDeviceId");

        InOrder order = inOrder(apiKeyService, jwtService);
        order.verify(apiKeyService).bindDeviceOrThrow(apiKey, null);
        order.verify(apiKeyService).incrementTokenVersion(1L);
        order.verify(jwtService).generateAccessToken(eq("client"), anyMap());
    }

    @Test
    void adminLoginWithBoundDeviceIdShouldOmitClaimAndIncrementTokenVersion() {
        ApiKey apiKey = adminApiKey(4L);
        when(apiKeyService.findByKeyValue(API_KEY_VALUE)).thenReturn(apiKey);
        when(apiKeyService.incrementTokenVersion(1L)).thenReturn(5L);
        when(jwtService.generateAccessToken(eq("client"), anyMap())).thenReturn("access.jwt");
        when(refreshTokenService.createRefreshToken(apiKey)).thenReturn("rt_new");

        var response =
                authService.authenticate(API_KEY_VALUE, new TokenRequest("device-a", "1.0.0"));

        assertThat(response.accessToken()).isEqualTo("access.jwt");
        Map<String, Object> claims = capturedClaims();
        assertThat(claims).containsEntry("tv", 5L).doesNotContainKey("boundDeviceId");

        InOrder order = inOrder(apiKeyService, jwtService);
        order.verify(apiKeyService).bindDeviceOrThrow(apiKey, "device-a");
        order.verify(apiKeyService).incrementTokenVersion(1L);
        order.verify(jwtService).generateAccessToken(eq("client"), anyMap());
    }

    @Test
    void adminRefreshShouldOmitBoundDeviceIdAndKeepTokenVersion() {
        ApiKey apiKey = adminApiKey(7L);
        when(refreshTokenService.rotateRefreshToken("rt_old"))
                .thenReturn(new RefreshTokenService.RotationResult(apiKey, "rt_rotated"));
        when(jwtService.generateAccessToken(eq("client"), anyMap())).thenReturn("access.jwt");

        var response = authService.refreshToken(new RefreshTokenRequest("rt_old"));

        assertThat(response.refreshToken()).isEqualTo("rt_rotated");
        assertThat(capturedClaims()).containsEntry("tv", 7L).doesNotContainKey("boundDeviceId");
        verify(apiKeyService, never()).incrementTokenVersion(anyLong());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedClaims() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(jwtService).generateAccessToken(eq("client"), captor.capture());
        return captor.getValue();
    }

    private static ApiKey apiKey(long tokenVersion) {
        return ApiKey.builder()
                .id(1L)
                .clientName("client")
                .userIdentifier("user")
                .platform(Platform.MOBILE)
                .role(Role.SERVICE)
                .active(true)
                .boundDeviceId("device-a")
                .tokenVersion(tokenVersion)
                .build();
    }

    private static ApiKey adminApiKey(long tokenVersion) {
        return ApiKey.builder()
                .id(1L)
                .clientName("client")
                .userIdentifier("user")
                .platform(Platform.MOBILE)
                .role(Role.ADMIN)
                .active(true)
                .boundDeviceId(null)
                .tokenVersion(tokenVersion)
                .build();
    }
}
