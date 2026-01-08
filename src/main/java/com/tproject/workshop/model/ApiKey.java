package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.security.SecureRandom;
import java.time.Instant;

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

    @Column(name = "key_value", nullable = false, unique = true, length = 60)
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

    @Column
    private String description;

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
}
