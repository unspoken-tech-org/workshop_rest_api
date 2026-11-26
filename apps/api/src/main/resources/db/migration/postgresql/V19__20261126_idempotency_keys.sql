-- ============================================================
-- V19 — Idempotency-Key storage
-- One row per (scope, key). scope = authenticated principal + HTTP method + path, so the
-- same key may be reused across clients and endpoints. A row is claimed as IN_PROGRESS with
-- a short expires_at (crash recovery), then COMPLETED with the stored response and the TTL.
-- The UNIQUE constraint is what serialises concurrent requests carrying the same key.
-- ============================================================

CREATE TABLE idempotency_keys (
    id                    BIGSERIAL PRIMARY KEY,
    scope                 VARCHAR(255) NOT NULL,
    idempotency_key       VARCHAR(255) NOT NULL,
    request_hash          CHAR(64)     NOT NULL,
    status                VARCHAR(16)  NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    response_status       INTEGER,
    response_content_type VARCHAR(100),
    response_body         TEXT,
    created_at            TIMESTAMPTZ  NOT NULL,
    expires_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_idempotency_keys_scope_key UNIQUE (scope, idempotency_key)
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
