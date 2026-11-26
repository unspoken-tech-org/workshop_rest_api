package com.tproject.workshop.integration.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Deterministic {@link Clock} instances for unit and integration tests. */
public final class TestClock {

    public static final Instant DEFAULT_INSTANT = Instant.parse("2026-01-15T10:00:00Z");
    public static final ZoneId DEFAULT_ZONE = ZoneOffset.UTC;
    public static final Clock FIXED = Clock.fixed(DEFAULT_INSTANT, DEFAULT_ZONE);

    private TestClock() {}

    public static Clock fixed(Instant instant) {
        return Clock.fixed(instant, DEFAULT_ZONE);
    }

    public static Clock fixed(String isoInstant) {
        return Clock.fixed(Instant.parse(isoInstant), DEFAULT_ZONE);
    }
}
