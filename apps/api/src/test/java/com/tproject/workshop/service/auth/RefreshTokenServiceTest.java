package com.tproject.workshop.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tproject.workshop.exception.InvalidTokenException;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.RefreshToken;
import com.tproject.workshop.model.Role;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.repository.RefreshTokenRepository;
import com.tproject.workshop.util.KeyGeneratorUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private ApiKeyRepository apiKeyRepository;

    @Spy private Clock clock = Clock.systemDefaultZone();

    @InjectMocks private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 3600L);
    }

    @Test
    void shouldCreateRefreshTokenHashedInDb() {
        ApiKey apiKey =
                ApiKey.builder()
                        .id(1L)
                        .clientName("client")
                        .userIdentifier("user")
                        .platform(Platform.MOBILE)
                        .role(Role.SERVICE)
                        .boundDeviceId("device-a")
                        .build();

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = refreshTokenService.createRefreshToken(apiKey);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).revokeByApiKeyId(eq(1L));
        verify(refreshTokenRepository).save(tokenCaptor.capture());

        RefreshToken saved = tokenCaptor.getValue();
        assertThat(token).startsWith("rt_");
        assertThat(saved.getToken()).isEqualTo(KeyGeneratorUtils.sha256Hex(token));
        assertThat(saved.getToken()).matches("^[0-9a-f]{64}$");
        assertThat(saved.getApiKey()).isEqualTo(apiKey);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void shouldRotateRefreshTokenAndIssueNewHashedToken() {
        String rawOldToken = "rt_old_token";
        String hashedOldToken = KeyGeneratorUtils.sha256Hex(rawOldToken);
        ApiKey apiKey = activeApiKey(10L, null);
        RefreshToken existing =
                RefreshToken.builder()
                        .id(42L)
                        .token(hashedOldToken)
                        .apiKey(apiKey)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .revoked(false)
                        .build();

        when(refreshTokenRepository.findByToken(hashedOldToken)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.revokeIfActive(42L)).thenReturn(1);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(rawOldToken);

        assertThat(result.apiKey()).isEqualTo(apiKey);
        assertThat(result.refreshToken()).startsWith("rt_").isNotEqualTo(rawOldToken);

        ArgumentCaptor<RefreshToken> savedCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).revokeIfActive(42L);
        verify(refreshTokenRepository).save(savedCaptor.capture());
        verify(apiKeyRepository, never()).incrementAndGetTokenVersion(anyLong());
        assertThat(savedCaptor.getValue().getToken())
                .isEqualTo(KeyGeneratorUtils.sha256Hex(result.refreshToken()));
    }

    @Test
    void shouldDetectRevokedTokenReuseAndRevokeAllTokensForApiKey() {
        String rawReusedToken = "rt_reused_token";
        String hashedReusedToken = KeyGeneratorUtils.sha256Hex(rawReusedToken);
        ApiKey apiKey = activeApiKey(15L, null);
        RefreshToken revokedToken =
                RefreshToken.builder()
                        .id(99L)
                        .token(hashedReusedToken)
                        .apiKey(apiKey)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .revoked(true)
                        .build();

        when(refreshTokenRepository.findByToken(hashedReusedToken))
                .thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawReusedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository).revokeByApiKeyId(15L);
        verify(apiKeyRepository).incrementAndGetTokenVersion(15L);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldDetectConcurrentReuseAndRevokeAllTokensForApiKey() {
        String rawToken = "rt_concurrent_token";
        String hashedToken = KeyGeneratorUtils.sha256Hex(rawToken);
        ApiKey apiKey = activeApiKey(16L, null);
        RefreshToken activeToken =
                RefreshToken.builder()
                        .id(100L)
                        .token(hashedToken)
                        .apiKey(apiKey)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .revoked(false)
                        .build();

        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(activeToken));
        when(refreshTokenRepository.revokeIfActive(100L)).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository).revokeByApiKeyId(16L);
        verify(apiKeyRepository).incrementAndGetTokenVersion(16L);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRejectRefreshWhenApiKeyIsExpired() {
        String rawToken = "rt_valid_token";
        String hashedToken = KeyGeneratorUtils.sha256Hex(rawToken);
        ApiKey expiredKey = activeApiKey(20L, Instant.now().minusSeconds(60));
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .id(77L)
                        .token(hashedToken)
                        .apiKey(expiredKey)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .revoked(false)
                        .build();

        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository, never()).revokeIfActive(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRejectRefreshWhenApiKeyIsInactive() {
        String rawToken = "rt_valid_token_inactive_key";
        String hashedToken = KeyGeneratorUtils.sha256Hex(rawToken);
        ApiKey inactiveKey =
                ApiKey.builder()
                        .id(21L)
                        .active(false)
                        .platform(Platform.MOBILE)
                        .role(Role.SERVICE)
                        .build();
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .id(78L)
                        .token(hashedToken)
                        .apiKey(inactiveKey)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .revoked(false)
                        .build();

        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("inactive");

        verify(refreshTokenRepository, never()).revokeIfActive(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldRevokeRefreshTokenAndIncrementTokenVersionOnLogout() {
        String rawToken = "rt_logout_token";
        String hashedToken = KeyGeneratorUtils.sha256Hex(rawToken);
        ApiKey apiKey = activeApiKey(30L, null);
        RefreshToken refreshToken =
                RefreshToken.builder()
                        .id(80L)
                        .token(hashedToken)
                        .apiKey(apiKey)
                        .expiresAt(Instant.now().plusSeconds(600))
                        .revoked(false)
                        .build();

        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revokeRefreshToken(rawToken);

        assertThat(refreshToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(refreshToken);
        verify(apiKeyRepository).incrementAndGetTokenVersion(30L);
    }

    @Test
    void shouldNotIncrementTokenVersionWhenLogoutTokenIsUnknown() {
        String rawToken = "rt_unknown_token";
        when(refreshTokenRepository.findByToken(KeyGeneratorUtils.sha256Hex(rawToken)))
                .thenReturn(Optional.empty());

        refreshTokenService.revokeRefreshToken(rawToken);

        verify(apiKeyRepository, never()).incrementAndGetTokenVersion(anyLong());
    }

    @Test
    void shouldNotIncrementTokenVersionWhenCreatingRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        refreshTokenService.createRefreshToken(activeApiKey(31L, null));

        verify(apiKeyRepository, never()).incrementAndGetTokenVersion(anyLong());
    }

    private static ApiKey activeApiKey(Long id, Instant expiresAt) {
        return ApiKey.builder()
                .id(id)
                .clientName("client")
                .userIdentifier("user")
                .platform(Platform.MOBILE)
                .role(Role.SERVICE)
                .active(true)
                .expiresAt(expiresAt)
                .boundDeviceId("device-a")
                .build();
    }
}
