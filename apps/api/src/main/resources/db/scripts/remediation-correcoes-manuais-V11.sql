-- ============================================================
-- Manual Audit & Remediation Script for V11 (Budget Fee)
-- Target migration: V11__14_06_2026_migrate_budget_fee.sql
--
-- IMPORTANT:
--   * This is NOT a Flyway migration. Do NOT place under db/migration/.
--   * Run the READ-ONLY AUDIT QUERIES first and inspect the results.
--   * Execute the REMEDIATION statements only inside a transaction
--     after validating the affected rows in QA/production backup.
-- ============================================================

-- ============================================================
-- PART 1: READ-ONLY AUDIT QUERIES
-- ============================================================

-- 1.1 Check migration_marker entries created by V11
SELECT id, migration_name, applied_at, description
FROM migration_marker
WHERE migration_name = 'V11__14_06_2026_migrate_budget_fee'
ORDER BY id;

-- 1.2 Summary of payments fabricated by V11 (linked via migration_marker_id)
SELECT
    COUNT(*) AS fabricated_payment_count,
    COALESCE(SUM(p.payment_value), 0) AS total_fabricated_value,
    MIN(p.created_at) AS first_inserted_at,
    MAX(p.created_at) AS last_inserted_at
FROM payments p
JOIN migration_marker mm ON mm.id = p.migration_marker_id
WHERE mm.migration_name = 'V11__14_06_2026_migrate_budget_fee';

-- 1.3 Detailed list of fabricated taxa_orcamento payments (payment_type = 'outro', payment_date = entry_date)
SELECT
    p.id AS payment_id,
    p.id_device AS device_id,
    d.device_status,
    d.entry_date,
    p.payment_date,
    p.payment_type,
    p.payment_value,
    p.category,
    d.budget_fee,
    d.service_value,
    p.migration_marker_id
FROM payments p
JOIN devices d ON d.id = p.id_device
JOIN migration_marker mm ON mm.id = p.migration_marker_id
WHERE mm.migration_name = 'V11__14_06_2026_migrate_budget_fee'
ORDER BY p.id_device, p.id;

-- 1.4 Audit devices with budget_fee_collected = true where total paid differs from service_value
SELECT
    d.id AS device_id,
    d.device_status,
    d.budget_fee,
    d.service_value,
    COALESCE(SUM(p.payment_value), 0) AS total_paid,
    COUNT(p.id) AS payment_count,
    COUNT(p.migration_marker_id) AS v11_fabricated_count
FROM devices d
LEFT JOIN payments p ON p.id_device = d.id
WHERE d.budget_fee_collected = true
  AND d.budget_fee > 0
GROUP BY d.id, d.device_status, d.budget_fee, d.service_value
HAVING COALESCE(SUM(p.payment_value), 0) <> COALESCE(d.service_value, 0)
ORDER BY d.id;

-- 1.5 Specific inspection for devices 5557 and 5222 referenced in V11 header
SELECT
    d.id AS device_id,
    d.device_status,
    d.entry_date,
    d.departure_date,
    d.budget_fee,
    d.budget_fee_collected,
    d.service_value,
    p.id AS payment_id,
    p.payment_date,
    p.payment_type,
    p.payment_value,
    p.category,
    p.migration_marker_id
FROM devices d
LEFT JOIN payments p ON p.id_device = d.id
WHERE d.id IN (5557, 5222)
ORDER BY d.id, p.id;

-- ============================================================
-- PART 2: MANUAL REMEDIATION TEMPLATE (REVIEW BEFORE RUNNING)
-- ============================================================
-- Wrap any manual cleanup in a transaction, verify row counts with
-- SELECT first, and COMMIT only after confirming the diff.
--
-- Example: Remove fabricated 'outro' taxa_orcamento payments created by V11
-- Step 5 (devices with no prior payments and service_value IS NULL or 0):
--
-- BEGIN;
--
-- DELETE FROM payments p
-- USING migration_marker mm, devices d
-- WHERE p.migration_marker_id = mm.id
--   AND p.id_device = d.id
--   AND mm.migration_name = 'V11__14_06_2026_migrate_budget_fee'
--   AND p.payment_type = 'outro'
--   AND p.category = 'taxa_orcamento'
--   AND (d.service_value IS NULL OR d.service_value = 0);
--
-- -- Verify remaining V11 marker payments before committing:
-- SELECT p.id, p.id_device, p.payment_value, p.payment_date, p.category
-- FROM payments p
-- JOIN migration_marker mm ON mm.id = p.migration_marker_id
-- WHERE mm.migration_name = 'V11__14_06_2026_migrate_budget_fee';
--
-- ROLLBACK; -- Change to COMMIT; only after verifying the affected rows.
