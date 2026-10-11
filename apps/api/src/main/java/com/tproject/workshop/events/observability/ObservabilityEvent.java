package com.tproject.workshop.events.observability;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable, validated event data that can be adapted to OTLP later.
 * Construction has no side effects and intentionally does not emit anything.
 */
public record ObservabilityEvent(
        ObservabilityEventContract contract,
        Instant occurredAt,
        Map<String, String> attributes) {

    public ObservabilityEvent {
        Objects.requireNonNull(contract, "contract must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(attributes, "attributes must not be null");

        ObservabilityEventValidator.validate(contract, attributes);
        attributes = Map.copyOf(attributes);
    }

    public static ObservabilityEvent of(
            String eventName,
            Instant occurredAt,
            Map<String, String> attributes) {
        return new ObservabilityEvent(
                ObservabilityEventCatalog.require(eventName),
                occurredAt,
                attributes);
    }

    public String name() {
        return contract.name();
    }

    public int version() {
        return contract.version();
    }

    public EventSeverity severity() {
        return contract.severity();
    }
}
