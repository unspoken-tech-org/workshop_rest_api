package com.tproject.workshop.events.observability;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Versioned schema metadata for one structured observability event.
 *
 * <p>This is deliberately a data contract only. It does not publish an
 * application event, create a log record or initialize an OTLP exporter.</p>
 */
public record ObservabilityEventContract(
        String name,
        int version,
        EventSeverity severity,
        Set<String> requiredAttributes,
        Set<String> optionalAttributes) {

    public ObservabilityEventContract {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(requiredAttributes, "requiredAttributes must not be null");
        Objects.requireNonNull(optionalAttributes, "optionalAttributes must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }

        Set<String> required = immutableNames(requiredAttributes, "requiredAttributes");
        Set<String> optional = immutableNames(optionalAttributes, "optionalAttributes");
        if (!Collections.disjoint(required, optional)) {
            throw new IllegalArgumentException("required and optional attributes must be disjoint");
        }
        requiredAttributes = required;
        optionalAttributes = optional;
    }

    public Set<String> allowedAttributes() {
        Set<String> allowed = new HashSet<>(requiredAttributes);
        allowed.addAll(optionalAttributes);
        return Collections.unmodifiableSet(allowed);
    }

    private static Set<String> immutableNames(Set<String> names, String field) {
        Set<String> copy = new HashSet<>();
        for (String name : names) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException(field + " must not contain blank names");
            }
            copy.add(name);
        }
        return Collections.unmodifiableSet(copy);
    }
}
