-- ============================================================
-- V15 — Foreign key, timeline, and pg_trgm GIN indexes
-- ============================================================

-- Immutable unaccent wrapper so expression GIN indexes can normalize accents.
CREATE OR REPLACE FUNCTION public.immutable_unaccent(text)
RETURNS text
LANGUAGE sql
IMMUTABLE PARALLEL SAFE STRICT
AS $$
    SELECT public.unaccent('public.unaccent', $1);
$$;

-- Foreign keys & operational query indexes
CREATE INDEX IF NOT EXISTS ix_devices_id_customer
    ON devices (id_customer);

CREATE INDEX IF NOT EXISTS ix_devices_id_brand_model_type
    ON devices (id_brand_model_type);

CREATE INDEX IF NOT EXISTS ix_devices_id_technician
    ON devices (id_technician);

CREATE INDEX IF NOT EXISTS ix_devices_last_viewed_at
    ON devices (last_viewed_at DESC)
    WHERE last_viewed_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_device_history_id_device
    ON device_history (id_device);

CREATE INDEX IF NOT EXISTS ix_device_history_history_date
    ON device_history (history_date);

CREATE INDEX IF NOT EXISTS ix_payments_id_device
    ON payments (id_device);

CREATE INDEX IF NOT EXISTS ix_customer_contact_id_device_last_contact
    ON customer_contact (id_device, last_contact DESC, id DESC);

CREATE INDEX IF NOT EXISTS ix_customer_phones_id_phone
    ON customer_phones (id_phone);

-- Trigram GIN indexes for ILIKE and pg_trgm (<% / word_similarity) searches
CREATE INDEX IF NOT EXISTS ix_customers_name_trgm
    ON customers USING gin (LOWER(public.immutable_unaccent(name)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_customers_email_trgm
    ON customers USING gin (LOWER(public.immutable_unaccent(email)) gin_trgm_ops)
    WHERE email IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_customers_cpf_trgm
    ON customers USING gin (cpf gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_phones_number_trgm
    ON phones USING gin ("number" gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_brands_brand_trgm
    ON brands USING gin (LOWER(public.immutable_unaccent(brand)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_models_model_trgm
    ON models USING gin (LOWER(public.immutable_unaccent(model)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS ix_types_type_trgm
    ON "types" USING gin (LOWER(public.immutable_unaccent("type")) gin_trgm_ops);
