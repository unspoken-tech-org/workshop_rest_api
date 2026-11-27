package com.tproject.workshop.idempotency;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain JDBC on purpose: every statement runs in its own (auto-commit) transaction, so a claim is
 * visible to concurrent requests immediately and is independent of the business transaction.
 */
@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {

    public record Row(long id, String requestHash, boolean completed, StoredResponse response) {}

    private final JdbcTemplate jdbc;

    /**
     * Atomically claims (scope, key). Inserts a new IN_PROGRESS row, or takes over a row whose
     * {@code expires_at} has passed. Returns the row id, or empty when a live row exists.
     */
    public Optional<Long> claim(
            String scope, String key, String hash, Instant now, Instant expiresAt) {
        return jdbc
                .queryForList(
                        """
                        INSERT INTO idempotency_keys
                            (scope, idempotency_key, request_hash, status, created_at, expires_at)
                        VALUES (?, ?, ?, 'IN_PROGRESS', ?, ?)
                        ON CONFLICT (scope, idempotency_key) DO UPDATE SET
                            request_hash = EXCLUDED.request_hash,
                            status = 'IN_PROGRESS',
                            response_status = NULL,
                            response_content_type = NULL,
                            response_body = NULL,
                            created_at = EXCLUDED.created_at,
                            expires_at = EXCLUDED.expires_at
                        WHERE idempotency_keys.expires_at <= EXCLUDED.created_at
                        RETURNING id
                        """,
                        Long.class,
                        scope,
                        key,
                        hash,
                        Timestamp.from(now),
                        Timestamp.from(expiresAt))
                .stream()
                .findFirst();
    }

    public Optional<Row> find(String scope, String key) {
        return jdbc
                .query(
                        """
                        SELECT id, request_hash, status, response_status,
                               response_content_type, response_body
                        FROM idempotency_keys
                        WHERE scope = ? AND idempotency_key = ?
                        """,
                        (rs, i) -> {
                            boolean completed = "COMPLETED".equals(rs.getString("status"));
                            StoredResponse response =
                                    completed
                                            ? new StoredResponse(
                                                    rs.getInt("response_status"),
                                                    rs.getString("response_content_type"),
                                                    rs.getString("response_body"))
                                            : null;
                            return new Row(
                                    rs.getLong("id"),
                                    rs.getString("request_hash"),
                                    completed,
                                    response);
                        },
                        scope,
                        key)
                .stream()
                .findFirst();
    }

    public void complete(long id, StoredResponse response, Instant expiresAt) {
        jdbc.update(
                """
                UPDATE idempotency_keys
                SET status = 'COMPLETED', response_status = ?, response_content_type = ?,
                    response_body = ?, expires_at = ?
                WHERE id = ?
                """,
                response.status(),
                response.contentType(),
                response.body(),
                Timestamp.from(expiresAt),
                id);
    }

    public void release(long id) {
        jdbc.update("DELETE FROM idempotency_keys WHERE id = ?", id);
    }

    public int deleteExpired(Instant now) {
        return jdbc.update(
                "DELETE FROM idempotency_keys WHERE expires_at <= ?", Timestamp.from(now));
    }
}
