package com.tproject.workshop.service.auth;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.RefreshToken;
import com.tproject.workshop.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 3600L);
    }

    @Test
    void shouldCreateRefreshTokenWithoutDeviceId() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .clientName("client")
                .userIdentifier("user")
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
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getApiKey()).isEqualTo(apiKey);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }
}
