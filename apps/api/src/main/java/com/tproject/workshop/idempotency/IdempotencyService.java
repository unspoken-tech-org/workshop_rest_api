package com.tproject.workshop.idempotency;

import com.tproject.workshop.exception.IdempotencyKeyReusedException;
import com.tproject.workshop.exception.IdempotencyRequestInProgressException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Storage, TTL and decision logic of {@code Idempotency-Key}, independent of any endpoint.
 *
 * <p>Outcomes of {@link #begin}: {@link Proceed} (this request owns the key and must run, then call
 * {@link #complete} or {@link #release}), {@link Replay} (a completed request exists, return its
 * stored response), or an exception: 422 when the key was used with a different body, 409 while the
 * first request is still running. A key whose TTL (or in-progress timeout) elapsed is free again.
 */
@Service
public class IdempotencyService {

    public sealed interface Outcome permits Proceed, Replay {}

    public record Proceed(long id) implements Outcome {}

    public record Replay(StoredResponse response) implements Outcome {}

    private final IdempotencyRepository repository;
    private final Clock clock;
    private final Duration ttl;
    private final Duration inProgressTimeout;

    public IdempotencyService(
            IdempotencyRepository repository,
            Clock clock,
            @Value("${app.idempotency.ttl:24h}") Duration ttl,
            @Value("${app.idempotency.in-progress-timeout:60s}") Duration inProgressTimeout) {
        this.repository = repository;
        this.clock = clock;
        this.ttl = ttl;
        this.inProgressTimeout = inProgressTimeout;
    }

    public Outcome begin(String scope, String key, String requestHash) {
        // Two attempts: a row may expire or be released between the claim and the read.
        for (int attempt = 0; attempt < 3; attempt++) {
            Instant now = clock.instant();
            var claimed =
                    repository.claim(scope, key, requestHash, now, now.plus(inProgressTimeout));
            if (claimed.isPresent()) {
                return new Proceed(claimed.get());
            }
            var existing = repository.find(scope, key);
            if (existing.isEmpty()) {
                continue;
            }
            var row = existing.get();
            if (!row.requestHash().equals(requestHash)) {
                throw new IdempotencyKeyReusedException();
            }
            if (!row.completed()) {
                throw new IdempotencyRequestInProgressException();
            }
            return new Replay(row.response());
        }
        throw new IdempotencyRequestInProgressException();
    }

    public void complete(long id, StoredResponse response) {
        repository.complete(id, response, clock.instant().plus(ttl));
    }

    public void release(long id) {
        repository.release(id);
    }

    public int purgeExpired() {
        return repository.deleteExpired(clock.instant());
    }
}
