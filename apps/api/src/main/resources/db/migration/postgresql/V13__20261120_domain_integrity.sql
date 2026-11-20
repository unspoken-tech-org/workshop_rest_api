-- ============================================================
-- V13 — Domain & Data Integrity
-- 1. Per-customer phone alias on customer_phones + backfill from phones
-- 2. Unique partial index on normalized CPF
-- 3. Optimistic locking version columns on devices, customers, payments
-- ============================================================

-- 1. Per-customer phone alias
ALTER TABLE customer_phones ADD COLUMN IF NOT EXISTS alias VARCHAR;

UPDATE customer_phones cp
SET alias = p.alias
FROM phones p
WHERE cp.id_phone = p.id
  AND cp.alias IS NULL
  AND p.alias IS NOT NULL;

-- 2. Unique partial index on normalized CPF (digits only, ignoring blank values)
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_cpf_unique
    ON customers (regexp_replace(cpf, '\D', '', 'g'))
    WHERE regexp_replace(cpf, '\D', '', 'g') <> '';

-- 3. Optimistic locking version columns
ALTER TABLE devices ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
