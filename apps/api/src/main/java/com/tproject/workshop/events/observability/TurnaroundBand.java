package com.tproject.workshop.events.observability;

import java.time.Duration;
import java.time.Instant;

/** Maps how long a device stayed in the workshop to a coarse band. */
public final class TurnaroundBand {

    private TurnaroundBand() {
    }

    public static String of(Instant entry, Instant closed) {
        if (entry == null || closed == null || closed.isBefore(entry)) {
            return "lt_1d";
        }
        long hours = Duration.between(entry, closed).toHours();
        if (hours < 24) {
            return "lt_1d";
        }
        if (hours < 72) {
            return "1d_3d";
        }
        if (hours < 168) {
            return "3d_7d";
        }
        return "gt_7d";
    }
}
