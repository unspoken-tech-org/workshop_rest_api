-- Clears device binding fields (bound_device_id, bound_at) for ADMIN API keys.
-- Idempotent: safe to run multiple times without side effects on already-clean rows.
--
-- Usage:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f scripts/sql/clear_admin_bound_device_id.sql

BEGIN;

UPDATE api_keys
SET bound_device_id = NULL,
    bound_at = NULL
WHERE role = 'ADMIN'
  AND (bound_device_id IS NOT NULL OR bound_at IS NOT NULL);

COMMIT;
