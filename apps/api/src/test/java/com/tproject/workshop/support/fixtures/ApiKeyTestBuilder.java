package com.tproject.workshop.support.fixtures;

import com.tproject.workshop.model.ApiKey;
import com.tproject.workshop.model.Platform;
import com.tproject.workshop.model.Role;
import java.time.Instant;

/** Test data builder for {@link ApiKey}. */
public final class ApiKeyTestBuilder {

    private Long id = 1L;
    private String clientName = "client";
    private String userIdentifier = "user";
    private Platform platform = Platform.MOBILE;
    private Role role = Role.SERVICE;
    private boolean active = true;
    private Instant expiresAt;
    private String boundDeviceId = "device-a";

    private ApiKeyTestBuilder() {}

    public static ApiKeyTestBuilder anApiKey() {
        return new ApiKeyTestBuilder();
    }

    public ApiKeyTestBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ApiKeyTestBuilder withActive(boolean active) {
        this.active = active;
        return this;
    }

    public ApiKeyTestBuilder withExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public ApiKeyTestBuilder withBoundDeviceId(String boundDeviceId) {
        this.boundDeviceId = boundDeviceId;
        return this;
    }

    public ApiKeyTestBuilder withRole(Role role) {
        this.role = role;
        return this;
    }

    public ApiKey build() {
        return ApiKey.builder()
                .id(id)
                .clientName(clientName)
                .userIdentifier(userIdentifier)
                .platform(platform)
                .role(role)
                .active(active)
                .expiresAt(expiresAt)
                .boundDeviceId(boundDeviceId)
                .build();
    }
}
