-- ============================================================
-- V16 — Payment & Device integrity constraints
-- 1. Enforce NOT NULL + DEFAULT on devices.budget_fee and budget_fee_collected
-- 2. Reconcile devices.budget_fee_collected from payments(category = 'taxa_orcamento')
-- 3. Add CHECK constraints (NOT VALID for safe rollout on legacy rows, enforced on all writes)
-- ============================================================

UPDATE devices
SET budget_fee = 0
WHERE budget_fee IS NULL;

ALTER TABLE devices
    ALTER COLUMN budget_fee SET DEFAULT 0,
    ALTER COLUMN budget_fee SET NOT NULL;

UPDATE devices
SET budget_fee_collected = false
WHERE budget_fee_collected IS NULL;

ALTER TABLE devices
    ALTER COLUMN budget_fee_collected SET DEFAULT false,
    ALTER COLUMN budget_fee_collected SET NOT NULL;

UPDATE devices d
SET budget_fee_collected = (
    d.budget_fee > 0
    AND COALESCE((
        SELECT SUM(p.payment_value)
        FROM payments p
        WHERE p.id_device = d.id
          AND LOWER(p.category) = 'taxa_orcamento'
    ), 0) >= d.budget_fee
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_payment_value_positive') THEN
        ALTER TABLE payments
            ADD CONSTRAINT chk_payments_payment_value_positive
            CHECK (payment_value > 0) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_payment_type_valid') THEN
        ALTER TABLE payments
            ADD CONSTRAINT chk_payments_payment_type_valid
            CHECK (LOWER(payment_type) IN ('credito', 'debito', 'dinheiro', 'pix', 'outro')) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_payments_category_valid') THEN
        ALTER TABLE payments
            ADD CONSTRAINT chk_payments_category_valid
            CHECK (LOWER(category) IN ('taxa_orcamento', 'servicos')) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_devices_budget_fee_non_negative') THEN
        ALTER TABLE devices
            ADD CONSTRAINT chk_devices_budget_fee_non_negative
            CHECK (budget_fee >= 0) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_devices_service_value_non_negative') THEN
        ALTER TABLE devices
            ADD CONSTRAINT chk_devices_service_value_non_negative
            CHECK (service_value IS NULL OR service_value >= 0) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_devices_status_valid') THEN
        ALTER TABLE devices
            ADD CONSTRAINT chk_devices_status_valid
            CHECK (device_status IN (
                'NOVO', 'EM_ANDAMENTO', 'AGUARDANDO', 'ENTREGUE',
                'DESCARTADO', 'APROVADO', 'NAO_APROVADO', 'PRONTO'
            )) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_devices_departure_after_entry') THEN
        ALTER TABLE devices
            ADD CONSTRAINT chk_devices_departure_after_entry
            CHECK (departure_date IS NULL OR departure_date >= entry_date) NOT VALID;
    END IF;
END $$;
