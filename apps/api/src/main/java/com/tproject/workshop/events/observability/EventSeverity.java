package com.tproject.workshop.events.observability;

/** Severity of a structured observability event; the application emitter writes ERROR as WARN. */
public enum EventSeverity {
    INFO,
    WARN,
    ERROR
}
