-- ============================================================
-- V17 — Immediate access-token revocation
-- token_version is embedded in every access token as the "tv" claim and is
-- compared on each request by JwtAuthenticationFilter. Incrementing it
-- (logout, refresh-token reuse detection, new login) invalidates every access
-- token issued before the increment.
-- ============================================================

ALTER TABLE api_keys
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
