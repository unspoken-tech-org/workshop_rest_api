package com.tproject.workshop.service.auth;

import com.tproject.workshop.dto.apikey.ApiKeyCreatedResponse;
import com.tproject.workshop.dto.apikey.ApiKeyResponse;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.ApiKeyDeviceBoundException;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.InvalidApiKeyException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.repository.RefreshTokenRepository;
import com.tproject.workshop.util.KeyGeneratorUtils;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObservabilityEventRecorder eventRecorder;

    /**
     * Validates if the API Key is valid and active. Updates the last used timestamp.
     *
     * @param apiKey the API key to be validated
     * @throws InvalidApiKeyException if the key is invalid, inactive, or expired
     */
    @Transactional
    public void validateApiKey(String apiKey) {
        ApiKey key = findByKeyValue(apiKey);

        // Check expiration
        if (key.getExpiresAt() != null && !key.getExpiresAt().isAfter(Instant.now())) {
            log.warn("Expired API Key for client: {} ({})", key.getClientName(), key.getPlatform());
            throw new InvalidApiKeyException("Expired API Key");
        }

        // Update last used timestamp
        apiKeyRepository.updateLastUsedAt(key.getKeyValue(), Instant.now());

        log.debug(
                "API Key validated for client: {} (platform: {})",
                key.getClientName(),
                key.getPlatform());
    }

    /**
     * Finds an active API Key by its plaintext value (compared via SHA-256 hash).
     *
     * @param apiKey the plaintext API key value
     * @return the ApiKey entity
     * @throws InvalidApiKeyException if not found or inactive
     */
    @Transactional(readOnly = true)
    public ApiKey findByKeyValue(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new InvalidApiKeyException("API Key is required");
        }
        String hashedKey = KeyGeneratorUtils.sha256Hex(apiKey);
        return apiKeyRepository
                .findByKeyValueAndActiveTrue(hashedKey)
                .orElseThrow(() -> {
                    log.warn("Invalid or inactive API Key");
                    return new InvalidApiKeyException("Invalid API Key");
                });
    }

    /** Binds the API Key to a device or validates an existing binding. */
    @Transactional
    public void bindDeviceOrThrow(ApiKey apiKey, String boundDeviceId) {
        if (boundDeviceId == null || boundDeviceId.isBlank()) {
            throw new BadRequestException("boundDeviceId é obrigatório");
        }

        if (apiKey.getBoundDeviceId() == null) {
            Instant now = Instant.now();
            int updated = apiKeyRepository.bindDevice(apiKey.getId(), boundDeviceId, now);
            if (updated == 0) {
                throw new ApiKeyDeviceBoundException("API Key já vinculada a outro dispositivo");
            }
            apiKey.setBoundDeviceId(boundDeviceId);
            apiKey.setBoundAt(apiKey.getBoundAt() == null ? now : apiKey.getBoundAt());
            log.info("API Key bound to device (apiKeyId: {})", apiKey.getId());
            Optional.ofNullable(apiKey.getPlatform())
                    .ifPresent(platform ->
                            eventRecorder.record(
                                    ObservabilityEventCatalog.API_KEY_DEVICE_BOUND,
                                    Map.of(
                                            "client.platform",
                                            platform.name().toLowerCase(Locale.ROOT))));
            return;
        }

        if (!apiKey.getBoundDeviceId().equals(boundDeviceId)) {
            log.warn("API Key já vinculada a outro dispositivo (apiKeyId: {})", apiKey.getId());
            throw new ApiKeyDeviceBoundException("API Key já vinculada a outro dispositivo");
        }
    }

    /** Clears device binding and revokes refresh tokens. */
    @Transactional
    public void resetDeviceBinding(Long apiKeyId) {
        ApiKey apiKey =
                apiKeyRepository
                        .findById(apiKeyId)
                        .orElseThrow(() ->
                                new NotFoundException("API Key not found with id: " + apiKeyId));

        apiKeyRepository.clearBoundDevice(apiKeyId);
        refreshTokenRepository.revokeByApiKeyId(apiKeyId);
        eventRecorder.record(ObservabilityEventCatalog.API_KEY_DEVICE_RESET, Map.of());

        log.info("API Key binding reset for id: {}", apiKeyId);
    }

    /**
     * Creates a new API Key for a client on a specific platform.
     *
     * @param clientName client name (Company)
     * @param userIdentifier user identifier (Technician/Device)
     * @param platform platform (MOBILE, WEB, DESKTOP, SERVER)
     * @param role role (ADMIN, SERVICE)
     * @param description optional description
     * @param expiresAt optional expiration date
     * @return the generated API Key with full key value (only shown once)
     */
    @Transactional
    public ApiKeyCreatedResponse createApiKey(
            String clientName,
            String userIdentifier,
            Platform platform,
            Role role,
            String description,
            Instant expiresAt) {
        if (apiKeyRepository.existsByClientNameAndUserIdentifierAndPlatformAndActiveTrue(
                clientName, userIdentifier, platform)) {
            log.warn(
                    "An active API Key already exists for client {} and user {} on platform {}",
                    clientName,
                    userIdentifier,
                    platform);
            throw new InvalidApiKeyException(
                    "An active API Key already exists for this client and user on this platform");
        }

        String rawKey = KeyGeneratorUtils.generateSecureKey(platform);
        String hashedKey = KeyGeneratorUtils.sha256Hex(rawKey);

        ApiKey apiKey =
                ApiKey.builder()
                        .keyValue(hashedKey)
                        .clientName(clientName)
                        .userIdentifier(userIdentifier)
                        .platform(platform)
                        .role(role != null ? role : Role.SERVICE)
                        .description(description)
                        .expiresAt(expiresAt)
                        .active(true)
                        .build();

        ApiKey savedKey = apiKeyRepository.save(apiKey);
        eventRecorder.record(
                ObservabilityEventCatalog.API_KEY_CREATED,
                Map.of(
                        "client.platform", platform.name().toLowerCase(Locale.ROOT),
                        "key.role", savedKey.getRole().name().toLowerCase(Locale.ROOT),
                        "key.expiry", expiresAt == null ? "none" : "set"));
        log.info(
                "API Key created for client: {}, user: {} (platform: {}, role: {})",
                clientName,
                userIdentifier,
                platform,
                apiKey.getRole());

        return ApiKeyCreatedResponse.fromEntity(savedKey, rawKey);
    }

    /**
     * Revokes (deactivates) an API Key.
     *
     * @param id API Key ID
     * @throws NotFoundException if API Key not found
     */
    @Transactional
    public void revokeApiKey(Long id) {
        if (!apiKeyRepository.existsById(id)) {
            throw new NotFoundException("API Key not found with id: " + id);
        }
        apiKeyRepository.deactivateById(id);
        eventRecorder.record(ObservabilityEventCatalog.API_KEY_REVOKED, Map.of("scope", "single"));
        log.info("API Key revoked: {}", id);
    }

    /**
     * Revokes all API Keys of a client.
     *
     * @param clientName client name
     */
    @Transactional
    public void revokeAllByClientName(String clientName) {
        if (apiKeyRepository.deactivateByClientName(clientName) > 0) {
            eventRecorder.record(
                    ObservabilityEventCatalog.API_KEY_REVOKED, Map.of("scope", "all_by_client"));
        }
        log.info("All API Keys revoked for client: {}", clientName);
    }

    /**
     * Returns all API Keys.
     *
     * @return list of all API Keys as DTOs with masked key values
     */
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> findAll() {
        return apiKeyRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(ApiKeyResponse::fromEntity)
                .toList();
    }

    /**
     * Finds an API Key by ID.
     *
     * @param id API Key ID
     * @return ApiKeyResponse with masked key value
     * @throws NotFoundException if API Key not found
     */
    @Transactional(readOnly = true)
    public ApiKeyResponse findById(Long id) {
        return apiKeyRepository
                .findById(id)
                .map(ApiKeyResponse::fromEntity)
                .orElseThrow(() -> new NotFoundException("API Key not found with id: " + id));
    }
}
