package com.tproject.workshop.events.observability;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Records a domain fact from a service. It never throws: observability must not change the outcome
 * of a business transaction.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ObservabilityEventRecorder {

    static final String ACTOR_ROLE = "actor.role";

    private final ApplicationEventPublisher publisher;

    public void record(ObservabilityEventContract contract, Map<String, String> attributes) {
        try {
            Map<String, String> enriched = new HashMap<>(attributes);
            if (contract.allowedAttributes().contains(ACTOR_ROLE)) {
                enriched.putIfAbsent(ACTOR_ROLE, currentActorRole());
            }
            publisher.publishEvent(
                    new PendingObservabilityEvent(contract, Instant.now(), Map.copyOf(enriched)));
        } catch (RuntimeException e) {
            log.warn("Observability event not recorded: {}", contract.name());
        }
    }

    static String currentActorRole() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(authentication ->
                        authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .map(ObservabilityEventRecorder::roleFromAuthority)
                                .filter(role -> !role.equals("anonymous"))
                                .findFirst()
                                .orElse("anonymous"))
                .orElse("anonymous");
    }

    private static String roleFromAuthority(String authority) {
        return switch (authority) {
            case "ROLE_ADMIN" -> "admin";
            case "ROLE_SERVICE" -> "service";
            default -> "anonymous";
        };
    }
}
