package com.tproject.workshop.service.auth;

import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.ApiKeyDeviceBoundException;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ObservabilityEventRecorder eventRecorder;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Test
    void shouldBindDeviceWhenUnbound() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .platform(Platform.MOBILE)
                .boundDeviceId(null)
                .build();

        when(apiKeyRepository.bindDevice(eq(1L), eq("device-a"), any(Instant.class)))
                .thenReturn(1);

        apiKeyService.bindDeviceOrThrow(apiKey, "device-a");

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(apiKeyRepository).bindDevice(eq(1L), eq("device-a"), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isNotNull();
        verify(eventRecorder).record(ObservabilityEventCatalog.API_KEY_DEVICE_BOUND,
                Map.of("client.platform", "mobile"));
        assertThat(apiKey.getBoundDeviceId()).isEqualTo("device-a");
        assertThat(apiKey.getBoundAt()).isNotNull();
    }

    @Test
    void shouldRejectBlankDeviceId() {
        ApiKey apiKey = ApiKey.builder().id(1L).build();

        assertThatThrownBy(() -> apiKeyService.bindDeviceOrThrow(apiKey, " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("boundDeviceId é obrigatório");

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void shouldRejectWhenBoundToAnotherDevice() {
        ApiKey apiKey = ApiKey.builder()
                .id(1L)
                .boundDeviceId("device-a")
                .build();

        assertThatThrownBy(() -> apiKeyService.bindDeviceOrThrow(apiKey, "device-b"))
                .isInstanceOf(ApiKeyDeviceBoundException.class)
                .hasMessageContaining("API Key já vinculada");

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void shouldResetDeviceBindingAndRevokeTokens() {
        ApiKey apiKey = ApiKey.builder()
                .id(2L)
                .boundDeviceId("device-x")
                .build();

        when(apiKeyRepository.findById(2L)).thenReturn(Optional.of(apiKey));

        apiKeyService.resetDeviceBinding(2L);

        verify(apiKeyRepository).clearBoundDevice(2L);
        verify(refreshTokenRepository).revokeByApiKeyId(2L);
        verify(eventRecorder).record(ObservabilityEventCatalog.API_KEY_DEVICE_RESET, Map.of());
    }

    @Test
    void shouldRecordSingleRevocation() {
        when(apiKeyRepository.existsById(3L)).thenReturn(true);

        apiKeyService.revokeApiKey(3L);

        verify(apiKeyRepository).deactivateById(3L);
        verify(eventRecorder).record(ObservabilityEventCatalog.API_KEY_REVOKED, Map.of("scope", "single"));
    }

    @Test
    void shouldRecordClientRevocationOnlyWhenKeysWereDeactivated() {
        when(apiKeyRepository.deactivateByClientName("client-a")).thenReturn(2);
        when(apiKeyRepository.deactivateByClientName("client-b")).thenReturn(0);

        apiKeyService.revokeAllByClientName("client-a");
        apiKeyService.revokeAllByClientName("client-b");

        verify(eventRecorder, times(1)).record(ObservabilityEventCatalog.API_KEY_REVOKED,
                Map.of("scope", "all_by_client"));
    }
}
