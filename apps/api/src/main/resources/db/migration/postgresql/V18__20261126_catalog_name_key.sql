-- ============================================================
-- V18 - name_key (chave normalizada) no catalogo: types, brands, models
--
-- name_key = lower + sem acento + so [a-z0-9]  ("Micro-ondas" = "microondas" = "micro ondas").
-- Substitui os indices unicos LOWER(TRIM(...)) da V5.
--
-- Esta migration FALHA de proposito se ainda houver cadastros com a mesma chave
-- (decisao 11 do PLANO_SEPARACAO_APARELHO_OS): ela so roda depois da limpeza do
-- catalogo (scripts/sql/catalog_cleanup_name_key.sql). Como o Flyway roda cada
-- migration em transacao, a falha nao altera nada.
-- A regra tem de bater com CatalogName.nameKey (Java); ver CatalogNameKeyIT.
-- ============================================================

DO $$
DECLARE
    rec RECORD;
    dup_total integer := 0;
    detail text := '';
BEGIN
    FOR rec IN
        SELECT 'types' AS catalog, key, array_agg(id ORDER BY id) AS ids
        FROM (SELECT id, regexp_replace(lower(public.immutable_unaccent("type")), '[^a-z0-9]', '', 'g') AS key
              FROM types) t
        GROUP BY key HAVING count(*) > 1
        UNION ALL
        SELECT 'brands', key, array_agg(id ORDER BY id)
        FROM (SELECT id, regexp_replace(lower(public.immutable_unaccent(brand)), '[^a-z0-9]', '', 'g') AS key
              FROM brands) b
        GROUP BY key HAVING count(*) > 1
        UNION ALL
        SELECT 'models', key, array_agg(id ORDER BY id)
        FROM (SELECT id, regexp_replace(lower(public.immutable_unaccent(model)), '[^a-z0-9]', '', 'g') AS key
              FROM models) m
        GROUP BY key HAVING count(*) > 1
    LOOP
        dup_total := dup_total + 1;
        IF dup_total <= 10 THEN
            detail := detail || format(E'\n  %s key=%L ids=%s', rec.catalog, rec.key, rec.ids);
        END IF;
    END LOOP;

    IF dup_total > 0 THEN
        RAISE EXCEPTION
            'V18 abortada: % grupo(s) de catalogo com a mesma name_key. Rode a limpeza do catalogo antes (scripts/sql/catalog_cleanup_name_key.sql). Primeiros grupos:%',
            dup_total, detail;
    END IF;
END
$$;

ALTER TABLE types
    ADD COLUMN IF NOT EXISTS name_key text
        GENERATED ALWAYS AS (regexp_replace(lower(public.immutable_unaccent("type")), '[^a-z0-9]', '', 'g')) STORED;
ALTER TABLE brands
    ADD COLUMN IF NOT EXISTS name_key text
        GENERATED ALWAYS AS (regexp_replace(lower(public.immutable_unaccent(brand)), '[^a-z0-9]', '', 'g')) STORED;
ALTER TABLE models
    ADD COLUMN IF NOT EXISTS name_key text
        GENERATED ALWAYS AS (regexp_replace(lower(public.immutable_unaccent(model)), '[^a-z0-9]', '', 'g')) STORED;

CREATE UNIQUE INDEX IF NOT EXISTS idx_types_name_key_unique ON types (name_key);
CREATE UNIQUE INDEX IF NOT EXISTS idx_brands_name_key_unique ON brands (name_key);
CREATE UNIQUE INDEX IF NOT EXISTS idx_models_name_key_unique ON models (name_key);

DROP INDEX IF EXISTS idx_types_type_unique_lower;
DROP INDEX IF EXISTS idx_brands_brand_unique_lower;
DROP INDEX IF EXISTS idx_models_model_unique_lower;
