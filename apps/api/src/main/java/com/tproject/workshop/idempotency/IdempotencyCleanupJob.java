package com.tproject.workshop.idempotency;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hourly purge of expired keys. Expired rows are also ignored on read, so this is housekeeping. */
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final IdempotencyService service;

    @Scheduled(initialDelayString = "PT1H", fixedDelayString = "PT1H")
    public void purgeExpired() {
        int removed = service.purgeExpired();
        if (removed > 0) {
            LOG.info("Purged {} expired idempotency keys", removed);
        }
    }
}
