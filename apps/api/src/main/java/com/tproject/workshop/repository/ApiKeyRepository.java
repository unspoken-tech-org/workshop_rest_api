package com.tproject.workshop.repository;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /** Finds an active API Key by its value. */
    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);

    /** Finds all active API Keys for a client. */
    List<ApiKey> findByClientNameAndActiveTrue(String clientName);

    /** Finds all active API Keys for a specific user within a client. */
    List<ApiKey> findByClientNameAndUserIdentifierAndActiveTrue(
            String clientName, String userIdentifier);

    /** Finds all active API Keys for a platform. */
    List<ApiKey> findByPlatformAndActiveTrue(Platform platform);

    /** Updates the last used timestamp of the API Key. */
    @Modifying
    @Query("UPDATE ApiKey a SET a.lastUsedAt = :lastUsedAt WHERE a.keyValue = :keyValue")
    void updateLastUsedAt(String keyValue, Instant lastUsedAt);

    /** Binds the API Key to a device if not bound or same device. */
    @Modifying
    @Query(
            "UPDATE ApiKey a SET a.boundDeviceId = :boundDeviceId, a.boundAt = COALESCE(a.boundAt, :boundAt) "
                    + "WHERE a.id = :id AND (a.boundDeviceId IS NULL OR a.boundDeviceId = :boundDeviceId)")
    int bindDevice(Long id, String boundDeviceId, Instant boundAt);

    /** Clears the bound device for an API Key. */
    @Modifying
    @Query("UPDATE ApiKey a SET a.boundDeviceId = null, a.boundAt = null WHERE a.id = :id")
    void clearBoundDevice(Long id);

    /** Deactivates an API Key (soft delete). */
    @Modifying
    @Query("UPDATE ApiKey a SET a.active = false WHERE a.id = :id")
    void deactivateById(Long id);

    /** Deactivates all API Keys for a client. */
    @Modifying
    @Query("UPDATE ApiKey a SET a.active = false WHERE a.clientName = :clientName")
    int deactivateByClientName(String clientName);

    /**
     * Atomically increments the access-token version and returns the value written by this
     * statement. Invalidates every access token issued with an older {@code tv} claim.
     *
     * <p>Not {@code @Modifying}: Spring Data would call {@code executeUpdate()}, which the
     * PostgreSQL driver rejects for a statement that returns rows ({@code RETURNING}). As a plain
     * query it is read as a single result. Must join a read-write transaction of the caller.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            value =
                    "UPDATE api_keys SET token_version = token_version + 1 WHERE id = :id"
                            + " RETURNING token_version",
            nativeQuery = true)
    long incrementAndGetTokenVersion(@Param("id") Long apiKeyId);

    /** Checks if there is an active API Key for the client, user and platform. */
    boolean existsByClientNameAndUserIdentifierAndPlatformAndActiveTrue(
            String clientName, String userIdentifier, Platform platform);
}
