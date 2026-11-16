package com.tproject.workshop.events.observability;

import java.time.Instant;
import java.util.Map;

/**
 * Event recorded inside a transaction; it is validated and written only after
 * the transaction commits, so a rollback never produces a domain fact.
 */
public record PendingObservabilityEvent(
        ObservabilityEventContract contract,
        Instant occurredAt,
        Map<String, String> attributes) {
}
