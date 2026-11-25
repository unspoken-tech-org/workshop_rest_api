package com.tproject.workshop.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tproject.workshop.dto.apikey.ApiKeyCreatedResponse;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.ApiKeyDeviceBoundException;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.EntityAlreadyExistsException;
import com.tproject.workshop.exception.InvalidApiKeyException;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.repository.RefreshTokenRepository;
import com.tproject.workshop.util.KeyGeneratorUtils;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock private ApiKeyRepository apiKeyRepository;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private ObservabilityEventRecorder eventRecorder;

    @InjectMocks private ApiKeyService apiKeyService;

    @Test
    void shouldBindDeviceWhenUnbound() {
        ApiKey apiKey =
                ApiKey.builder().id(1L).platform(Platform.MOBILE).boundDeviceId(null).build();

        when(apiKeyRepository.bindDevice(eq(1L), eq("device-a"), any(Instant.class))).thenReturn(1);

        apiKeyService.bindDeviceOrThrow(apiKey, "device-a");

        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(apiKeyRepository).bindDevice(eq(1L), eq("device-a"), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isNotNull();
        verify(eventRecorder)
                .record(
                        ObservabilityEventCatalog.API_KEY_DEVICE_BOUND,
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
        ApiKey apiKey = ApiKey.builder().id(1L).boundDeviceId("device-a").build();

        assertThatThrownBy(() -> apiKeyService.bindDeviceOrThrow(apiKey, "device-b"))
                .isInstanceOf(ApiKeyDeviceBoundException.class)
                .hasMessageContaining("API Key já vinculada");

        verifyNoInteractions(apiKeyRepository);
    }

    @Test
    void shouldResetDeviceBindingAndRevokeTokens() {
        ApiKey apiKey = ApiKey.builder().id(2L).boundDeviceId("device-x").build();

        when(apiKeyRepository.findById(2L)).thenReturn(Optional.of(apiKey));

        apiKeyService.resetDeviceBinding(2L);

        verify(apiKeyRepository).clearBoundDevice(2L);
        verify(apiKeyRepository).incrementAndGetTokenVersion(2L);
        verify(refreshTokenRepository).revokeByApiKeyId(2L);
        verify(eventRecorder).record(ObservabilityEventCatalog.API_KEY_DEVICE_RESET, Map.of());
    }

    @Test
    void shouldSkipDeviceBindingForAdminWhenDeviceIdIsNullOrBlank() {
        ApiKey adminKey =
                ApiKey.builder().id(1L).platform(Platform.MOBILE).role(Role.ADMIN).build();

        apiKeyService.bindDeviceOrThrow(adminKey, null);
        apiKeyService.bindDeviceOrThrow(adminKey, "   ");

        assertThat(adminKey.getBoundDeviceId()).isNull();
        assertThat(adminKey.getBoundAt()).isNull();
        verifyNoInteractions(apiKeyRepository, eventRecorder);
    }

    @Test
    void shouldSkipDeviceBindingForAdminEvenWhenAlreadyBoundToAnotherDevice() {
        Instant originalBoundAt = Instant.parse("2026-01-01T00:00:00Z");
        ApiKey adminKey =
                ApiKey.builder()
                        .id(1L)
                        .platform(Platform.MOBILE)
                        .role(Role.ADMIN)
                        .boundDeviceId("device-a")
                        .boundAt(originalBoundAt)
                        .build();

        apiKeyService.bindDeviceOrThrow(adminKey, "device-b");

        assertThat(adminKey.getBoundDeviceId()).isEqualTo("device-a");
        assertThat(adminKey.getBoundAt()).isEqualTo(originalBoundAt);
        verifyNoInteractions(apiKeyRepository, eventRecorder);
    }

    @Test
    void shouldRecordSingleRevocation() {
        when(apiKeyRepository.existsById(3L)).thenReturn(true);

        apiKeyService.revokeApiKey(3L);

        verify(apiKeyRepository).deactivateById(3L);
        verify(eventRecorder)
                .record(ObservabilityEventCatalog.API_KEY_REVOKED, Map.of("scope", "single"));
    }

    @Test
    void shouldRecordClientRevocationOnlyWhenKeysWereDeactivated() {
        when(apiKeyRepository.deactivateByClientName("client-a")).thenReturn(2);
        when(apiKeyRepository.deactivateByClientName("client-b")).thenReturn(0);

        apiKeyService.revokeAllByClientName("client-a");
        apiKeyService.revokeAllByClientName("client-b");

        verify(eventRecorder, times(1))
                .record(
                        ObservabilityEventCatalog.API_KEY_REVOKED,
                        Map.of("scope", "all_by_client"));
    }

    @Test
    void shouldStoreSha256HashOnCreateAndReturnPlaintextOnce() {
        when(apiKeyRepository.existsByClientNameAndUserIdentifierAndPlatformAndActiveTrue(
                        "client-a", "user-1", Platform.MOBILE))
                .thenReturn(false);
        when(apiKeyRepository.save(any(ApiKey.class)))
                .thenAnswer(invocation -> {
                    ApiKey entity = invocation.getArgument(0);
                    entity.setId(99L);
                    return entity;
                });

        ApiKeyCreatedResponse response =
                apiKeyService.createApiKey(
                        "client-a", "user-1", Platform.MOBILE, Role.SERVICE, "desc", null);

        ArgumentCaptor<ApiKey> savedCaptor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(savedCaptor.capture());

        String storedHash = savedCaptor.getValue().getKeyValue();
        assertThat(response.keyValue()).startsWith("sk_mobile_");
        assertThat(storedHash).isEqualTo(KeyGeneratorUtils.sha256Hex(response.keyValue()));
        assertThat(storedHash).matches("^[0-9a-f]{64}$");
    }

    @Test
    void shouldLookupBySha256HashAndNotLogKeyPrefixWhenInvalid() {
        String rawKey = "sk_mobile_SECRET_PREFIX_1234567890";
        String expectedHash = KeyGeneratorUtils.sha256Hex(rawKey);
        when(apiKeyRepository.findByKeyValueAndActiveTrue(expectedHash))
                .thenReturn(Optional.empty());

        Logger logger = (Logger) LoggerFactory.getLogger(ApiKeyService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> apiKeyService.findByKeyValue(rawKey))
                    .isInstanceOf(InvalidApiKeyException.class);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(apiKeyRepository).findByKeyValueAndActiveTrue(expectedHash);
        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(msg -> {
                    assertThat(msg).doesNotContain("sk_mobile_");
                    assertThat(msg).doesNotContain("SECRET");
                    assertThat(msg).doesNotContain("7890");
                });
    }

    @Test
    void shouldThrowEntityAlreadyExistsWhenCreatingDuplicateActiveKey() {
        when(apiKeyRepository.existsByClientNameAndUserIdentifierAndPlatformAndActiveTrue(
                        "client-a", "user-1", Platform.MOBILE))
                .thenReturn(true);

        assertThatThrownBy(() ->
                        apiKeyService.createApiKey(
                                "client-a", "user-1", Platform.MOBILE, Role.SERVICE, "desc", null))
                .isInstanceOf(EntityAlreadyExistsException.class);
        verify(apiKeyRepository, never()).save(any());
    }
}
