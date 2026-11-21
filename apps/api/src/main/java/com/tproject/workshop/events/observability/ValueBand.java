package com.tproject.workshop.events.observability;

import java.math.BigDecimal;

/** Maps an exact amount (BRL) to a coarse band; exact values never reach events. */
public final class ValueBand {

    private static final BigDecimal LOW_LIMIT = new BigDecimal("100");
    private static final BigDecimal MEDIUM_LIMIT = new BigDecimal("500");

    private ValueBand() {}

    public static String of(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return "zero";
        }
        if (value.compareTo(LOW_LIMIT) < 0) {
            return "low";
        }
        if (value.compareTo(MEDIUM_LIMIT) < 0) {
            return "medium";
        }
        return "high";
    }
}
