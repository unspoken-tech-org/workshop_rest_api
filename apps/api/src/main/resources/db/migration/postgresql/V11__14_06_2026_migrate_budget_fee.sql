-- ============================================================
-- V11 — Budget Fee
-- (a) Blanket UPDATE: category '' → 'servicos'
-- (b) RENAME: labor_value → budget_fee, labor_value_collected → budget_fee_collected
-- (c) DELETE duplicates (Pattern 4)
-- (d) INSERT taxa_orcamento for paid < service_value
-- (e) INSERT taxa_orcamento for devices with no payments
-- (f) RECATEGORIZE servicos → taxa_orcamento
-- (g) SPLIT single payment
--
-- PASSOS 8 E 9 (correcoes pontuais devices 5557/5222) MOVIDOS PARA:
-- src/main/resources/db/scripts/remediation-correcoes-manuais-V11.sql
-- Executar manualmente APOS validacao da migration em QA.
-- ============================================================

-- ============================================================
-- STEP 1: Infrastructure (migration_marker + blanket UPDATE)
-- ============================================================

-- 1a: Create migration_marker table
CREATE TABLE IF NOT EXISTS migration_marker (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    migration_name VARCHAR(255) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT NOW(),
    description TEXT
);

-- 1b: Blanket UPDATE '' → servicos
-- Converts all empty categories to 'servicos' (default category).
UPDATE payments p
SET category = 'servicos'
WHERE p.category = '';

-- 1c: Add migration_marker_id to payments
ALTER TABLE payments ADD COLUMN IF NOT EXISTS migration_marker_id INT;

-- 1d: FK constraint (with DO $$ guard — PostgreSQL does not support IF NOT EXISTS)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_payments_migration_marker'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_migration_marker
            FOREIGN KEY (migration_marker_id) REFERENCES migration_marker(id);
    END IF;
END $$;

-- ============================================================
-- STEP 2: RENAME columns
-- ============================================================
ALTER TABLE devices RENAME COLUMN labor_value TO budget_fee;
ALTER TABLE devices RENAME COLUMN labor_value_collected TO budget_fee_collected;

-- ============================================================
-- STEP 3: DELETE duplicates (Pattern 4)
-- Devices with paid>sv, budget_fee>0, budget_fee_collected=true,
-- 2 payments with the same value (double-click duplicate).
-- Keeps p1 (lower ID), deletes p2 (higher ID).
-- ============================================================
DELETE FROM payments
WHERE id IN (
    SELECT p2.id
    FROM payments p1
    JOIN payments p2
      ON p1.id_device = p2.id_device
     AND p1.id < p2.id
     AND p1.payment_value = p2.payment_value
    JOIN devices d ON d.id = p1.id_device
    WHERE d.budget_fee_collected = true
      AND d.budget_fee > 0
      AND d.service_value IS NOT NULL
      AND d.service_value > 0
      AND d.service_value < COALESCE(
          (SELECT SUM(payment_value) FROM payments WHERE id_device = d.id), 0)
);

-- ============================================================
-- STEP 4: INSERT taxa_orcamento — paid < service_value
-- service_value>0, paid < service_value.
-- Idempotent: NOT EXISTS ensures no duplicates on re-run.
-- ============================================================
WITH marker AS (
    INSERT INTO migration_marker (migration_name, description)
    VALUES ('V11__14_06_2026_migrate_budget_fee',
            'Inserts retroactive taxa_orcamento for devices with paid < service_value')
    RETURNING id
)
INSERT INTO payments (id_device, payment_date, payment_type, payment_value, category,
                      migration_marker_id, created_at, updated_at)
SELECT
    d.id,
    d.entry_date,
    'outro',
    d.budget_fee,
    'taxa_orcamento',
    (SELECT id FROM marker),
    NOW(),
    NOW()
FROM devices d
WHERE d.budget_fee_collected = true
  AND d.budget_fee > 0
  AND d.service_value IS NOT NULL
  AND d.service_value > 0
  AND COALESCE((SELECT SUM(payment_value) FROM payments WHERE id_device = d.id), 0) < d.service_value
  AND NOT EXISTS (
      SELECT 1 FROM payments p
      WHERE p.id_device = d.id
        AND p.category = 'taxa_orcamento'
        AND p.payment_value = d.budget_fee
  );

-- ============================================================
-- STEP 5: INSERT taxa_orcamento — no payments
-- budget_fee>0 and no payments recorded.
-- Idempotent: double NOT EXISTS ensures no duplicates on re-run.
-- ============================================================
INSERT INTO payments (id_device, payment_date, payment_type, payment_value, category,
                      migration_marker_id, created_at, updated_at)
SELECT
    d.id,
    d.entry_date,
    'outro',
    d.budget_fee,
    'taxa_orcamento',
    (SELECT id FROM migration_marker
     WHERE migration_name = 'V11__14_06_2026_migrate_budget_fee' LIMIT 1),
    NOW(),
    NOW()
FROM devices d
WHERE d.budget_fee_collected = true
  AND d.budget_fee > 0
  AND NOT EXISTS (
      SELECT 1 FROM payments p WHERE p.id_device = d.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM payments p2
      WHERE p2.id_device = d.id
        AND p2.category = 'taxa_orcamento'
        AND p2.payment_value = d.budget_fee
  );

-- ============================================================
-- STEP 6: RECATEGORIZE servicos → taxa_orcamento
-- servicos payment with value=budget_fee, no taxa_orcamento exists.
-- Idempotent: NOT EXISTS ensures devices that already have
-- taxa_orcamento(fee) are not affected.
-- ============================================================
UPDATE payments
SET category = 'taxa_orcamento',
    updated_at = NOW()
FROM devices d
WHERE payments.id_device = d.id
  AND d.budget_fee_collected = true
  AND d.budget_fee > 0
  AND d.service_value IS NOT NULL
  AND d.service_value > 0
  AND payments.category = 'servicos'
  AND payments.payment_value = d.budget_fee
  AND NOT EXISTS (
      SELECT 1 FROM payments p2
      WHERE p2.id_device = d.id
        AND p2.category = 'taxa_orcamento'
        AND p2.payment_value = d.budget_fee
  );

-- ============================================================
-- STEP 7: SPLIT single payment (fee embedded)
-- Splits: INSERT taxa_orcamento + UPDATE existing payment.
-- Idempotent: EXISTS ensures 7a ran before 7b.
-- ============================================================

-- 7a: INSERT taxa_orcamento
INSERT INTO payments (id_device, payment_date, payment_type, payment_value, category,
                      migration_marker_id, created_at, updated_at)
SELECT
    d.id,
    d.entry_date,
    'outro',
    d.budget_fee,
    'taxa_orcamento',
    (SELECT id FROM migration_marker
     WHERE migration_name = 'V11__14_06_2026_migrate_budget_fee' LIMIT 1),
    NOW(),
    NOW()
FROM devices d
WHERE d.budget_fee_collected = true
  AND d.budget_fee > 0
  AND d.service_value IS NOT NULL
  AND d.service_value > 0
  AND d.service_value = COALESCE(
      (SELECT SUM(payment_value) FROM payments WHERE id_device = d.id), 0)
  AND NOT EXISTS (
      SELECT 1 FROM payments p2
      WHERE p2.id_device = d.id
        AND p2.category = 'taxa_orcamento'
        AND p2.payment_value = d.budget_fee
  )
  AND NOT EXISTS (
      SELECT 1 FROM payments p3
      WHERE p3.id_device = d.id
        AND p3.category = 'servicos'
        AND p3.payment_value = d.budget_fee
  );

-- 7b: UPDATE existing payment to sv - budget_fee
UPDATE payments
SET payment_value = d.service_value - d.budget_fee,
    updated_at = NOW()
FROM devices d
WHERE payments.id_device = d.id
  AND d.budget_fee_collected = true
  AND d.budget_fee > 0
  AND d.service_value IS NOT NULL
  AND d.service_value > 0
  AND payments.payment_value = d.service_value
  AND payments.category <> 'taxa_orcamento'
  AND EXISTS (
      SELECT 1 FROM payments p2
      WHERE p2.id_device = d.id
        AND p2.category = 'taxa_orcamento'
        AND p2.payment_value = d.budget_fee
  );
