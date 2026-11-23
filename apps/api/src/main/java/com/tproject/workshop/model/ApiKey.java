package com.tproject.workshop.model;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false, unique = true, length = 64)
    private String keyValue;

    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;

    @Column(name = "user_identifier", nullable = false, length = 100)
    private String userIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.SERVICE;

    @Column private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "bound_device_id", length = 100)
    private String boundDeviceId;

    @Column(name = "bound_at")
    private Instant boundAt;

    /**
     * Access-token version (JWT claim {@code tv}). Written only by {@code
     * ApiKeyRepository#incrementAndGetTokenVersion}: excluded from Hibernate inserts/updates so a
     * dirty-checked stale entity can never roll the counter back.
     */
    @Column(name = "token_version", nullable = false, insertable = false, updatable = false)
    @Builder.Default
    private long tokenVersion = 0L;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
