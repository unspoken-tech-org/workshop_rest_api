package com.tproject.workshop.repository;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Finds an active API Key by its value.
     */
    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);

    /**
     * Finds all active API Keys for a client.
     */
    List<ApiKey> findByClientNameAndActiveTrue(String clientName);

    /**
     * Finds all active API Keys for a specific user within a client.
     */
    List<ApiKey> findByClientNameAndUserIdentifierAndActiveTrue(String clientName, String userIdentifier);

    /**
     * Finds all active API Keys for a platform.
     */
    List<ApiKey> findByPlatformAndActiveTrue(Platform platform);

    /**
     * Updates the last used timestamp of the API Key.
     */
    @Modifying
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :lastUsedAt WHERE a.keyValue = :keyValue")
    void updateLastUsedAt(String keyValue, Instant lastUsedAt);

    /**
     * Deactivates an API Key (soft delete).
     */
    @Modifying
    @Query("UPDATE ApiKey a SET a.active = false WHERE a.id = :id")
    void deactivateById(Long id);

    /**
     * Deactivates all API Keys for a client.
     */
    @Modifying
    @Query("UPDATE ApiKey a SET a.active = false WHERE a.clientName = :clientName")
    void deactivateByClientName(String clientName);

    /**
     * Checks if there is an active API Key for the client, user and platform.
     */
    boolean existsByClientNameAndUserIdentifierAndPlatformAndActiveTrue(String clientName, String userIdentifier, Platform platform);
}
