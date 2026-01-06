package com.tproject.workshop.service.auth;

import com.tproject.workshop.dto.apikey.ApiKeyCreatedResponse;
import com.tproject.workshop.dto.apikey.ApiKeyResponse;
import com.tproject.workshop.exception.InvalidApiKeyException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.repository.ApiKeyRepository;
import com.tproject.workshop.util.KeyGeneratorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Validates if the API Key is valid and active.
     * Updates the last used timestamp.
     *
     * @param apiKey the API key to be validated
     * @throws InvalidApiKeyException if the key is invalid, inactive, or expired
     */
    @Transactional
    public void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API Key missing in request");
            throw new InvalidApiKeyException("API Key is required");
        }

        ApiKey key = apiKeyRepository.findByKeyValueAndActiveTrue(apiKey)
                .orElseThrow(() -> {
                    log.warn("Invalid or inactive API Key: {}", maskApiKey(apiKey));
                    return new InvalidApiKeyException("Invalid API Key");
                });

        // Check expiration
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Expired API Key for client: {} ({})",
                    key.getClientName(), key.getPlatform());
            throw new InvalidApiKeyException("Expired API Key");
        }

        // Update last used timestamp
        apiKeyRepository.updateLastUsedAt(apiKey, Instant.now());

        log.debug("API Key validated for client: {} (platform: {})",
                key.getClientName(), key.getPlatform());
    }

    /**
     * Returns the client identifier based on the API Key.
     *
     * @param apiKey the API key
     * @return the client name associated with the key
     */
    @Transactional(readOnly = true)
    public String getClientId(String apiKey) {
        return apiKeyRepository.findByKeyValueAndActiveTrue(apiKey)
                .map(ApiKey::getClientName)
                .orElse("unknown_" + apiKey.substring(0, Math.min(8, apiKey.length())));
    }

    /**
     * Returns the platform associated with the API Key.
     *
     * @param apiKey the API key
     * @return the platform or null if not found
     */
    @Transactional(readOnly = true)
    public Platform getPlatform(String apiKey) {
        return apiKeyRepository.findByKeyValueAndActiveTrue(apiKey)
                .map(ApiKey::getPlatform)
                .orElse(null);
    }

    /**
     * Creates a new API Key for a client on a specific platform.
     *
     * @param clientName  client name
     * @param platform    platform (MOBILE, WEB, DESKTOP, SERVER)
     * @param description optional description
     * @param expiresAt   optional expiration date
     * @return the generated API Key with full key value (only shown once)
     */
    @Transactional
    public ApiKeyCreatedResponse createApiKey(String clientName, Platform platform,
                                              String description, Instant expiresAt) {
        if (apiKeyRepository.existsByClientNameAndPlatformAndActiveTrue(clientName, platform)) {
            log.warn("An API Key already exists for client {} on platform {}",
                    clientName, platform);
            throw new InvalidApiKeyException(
                    "An API Key already exists for this client on this platform");
        }

        ApiKey apiKey = ApiKey.builder()
                .keyValue(KeyGeneratorUtils.generateSecureKey(platform))
                .clientName(clientName)
                .platform(platform)
                .description(description)
                .expiresAt(expiresAt)
                .active(true)
                .build();

        ApiKey savedKey = apiKeyRepository.save(apiKey);
        log.info("API Key created for client: {} (platform: {})",
                clientName, platform);

        return ApiKeyCreatedResponse.fromEntity(savedKey);
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
        log.info("API Key revoked: {}", id);
    }

    /**
     * Revokes all API Keys of a client.
     *
     * @param clientName client name
     */
    @Transactional
    public void revokeAllByClientName(String clientName) {
        apiKeyRepository.deactivateByClientName(clientName);
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
        return apiKeyRepository.findById(id)
                .map(ApiKeyResponse::fromEntity)
                .orElseThrow(() -> new NotFoundException("API Key not found with id: " + id));
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
