-- Limpeza do catalogo (types, brands, models) ANTES da migration V18 (name_key).
-- Plano: PLANO_SEPARACAO_APARELHO_OS, decisao 11 (Fase 0).
--
-- O que faz: agrupa cadastros pela mesma chave normalizada (minusculas, sem acento,
-- so [a-z0-9]; a mesma regra da coluna name_key), escolhe o canonico de cada grupo
-- (o mais usado em devices; empate = menor id), reaponta brands_models_types, funde
-- as combinacoes tipo/marca/modelo que passam a coincidir (reapontando devices) e
-- apaga os cadastros repetidos.
--
-- Seguro para repetir: sem grupos repetidos, nada muda. Depende de
-- public.immutable_unaccent (V15). NAO precisa da V18 aplicada.
--
-- PASSOS (rodar primeiro em QA com copia mascarada, depois em PRD):
--   1. Backup/snapshot imediatamente antes (pg_dump ou PITR marcado).
--   2. Rode como esta: termina em ROLLBACK (ensaio). Revise os blocos "ANTES",
--      "GRUPOS" e "DEPOIS". Os 12 grupos de tipos e 7 de marcas sao conferidos a
--      mao; os de modelos, por amostragem (bloco "AMOSTRA DE MODELOS").
--   3. Se estiver certo, troque o ROLLBACK final por COMMIT e rode de novo.
--   4. So entao publique a versao com a V18 (ela aborta se restar duplicado).
--
-- Uso:  psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f scripts/sql/catalog_cleanup_name_key.sql

BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '120s';
LOCK TABLE types, brands, models, brands_models_types, devices IN SHARE ROW EXCLUSIVE MODE;

\echo '== ANTES: contagens =='
SELECT (SELECT count(*) FROM types)               AS types,
       (SELECT count(*) FROM brands)              AS brands,
       (SELECT count(*) FROM models)              AS models,
       (SELECT count(*) FROM brands_models_types) AS brands_models_types,
       (SELECT count(*) FROM devices)             AS devices,
       (SELECT count(*) FROM devices WHERE id_brand_model_type IS NOT NULL) AS devices_com_catalogo;

-- Mapa cadastro -> canonico, por catalogo.
CREATE TEMP TABLE catalog_map (
    catalog text NOT NULL,
    old_id integer NOT NULL,
    canonical_id integer NOT NULL,
    nome text NOT NULL,
    usos integer NOT NULL,
    PRIMARY KEY (catalog, old_id)
) ON COMMIT DROP;

INSERT INTO catalog_map (catalog, old_id, canonical_id, nome, usos)
SELECT catalog, id,
       first_value(id) OVER (PARTITION BY catalog, key ORDER BY usos DESC, id),
       nome, usos
FROM (
    SELECT 'types' AS catalog, t.id, t.type AS nome,
           regexp_replace(lower(public.immutable_unaccent(t.type)), '[^a-z0-9]', '', 'g') AS key,
           (SELECT count(*) FROM devices d JOIN brands_models_types b ON b.id = d.id_brand_model_type
             WHERE b.id_type = t.id)::int AS usos
    FROM types t
    UNION ALL
    SELECT 'brands', x.id, x.brand,
           regexp_replace(lower(public.immutable_unaccent(x.brand)), '[^a-z0-9]', '', 'g'),
           (SELECT count(*) FROM devices d JOIN brands_models_types b ON b.id = d.id_brand_model_type
             WHERE b.id_brand = x.id)::int
    FROM brands x
    UNION ALL
    SELECT 'models', m.id, m.model,
           regexp_replace(lower(public.immutable_unaccent(m.model)), '[^a-z0-9]', '', 'g'),
           (SELECT count(*) FROM devices d JOIN brands_models_types b ON b.id = d.id_brand_model_type
             WHERE b.id_model = m.id)::int
    FROM models m
) all_entries;

-- Fica so quem pertence a um grupo com mais de um cadastro.
DELETE FROM catalog_map cm
WHERE (cm.catalog, cm.canonical_id) IN (
    SELECT catalog, canonical_id FROM catalog_map GROUP BY catalog, canonical_id HAVING count(*) = 1);

\echo
\echo '== GRUPOS: resumo por catalogo =='
SELECT catalog, count(DISTINCT canonical_id) AS grupos, count(*) AS cadastros_envolvidos,
       count(*) - count(DISTINCT canonical_id) AS cadastros_a_apagar
FROM catalog_map GROUP BY catalog ORDER BY catalog;

\echo
\echo '== GRUPOS: tipos e marcas (revisar a mao; * = canonico) =='
SELECT catalog,
       string_agg(format('%s%s [%s] x%s', CASE WHEN old_id = canonical_id THEN '*' ELSE '' END,
                         nome, old_id, usos), ' | ' ORDER BY (old_id = canonical_id) DESC, old_id) AS grafias
FROM catalog_map
WHERE catalog IN ('types', 'brands')
GROUP BY catalog, canonical_id
ORDER BY catalog, canonical_id;

\echo
\echo '== AMOSTRA DE MODELOS: 25 grupos aleatorios (revisar por amostragem) =='
SELECT string_agg(format('%s%s [%s] x%s', CASE WHEN old_id = canonical_id THEN '*' ELSE '' END,
                         nome, old_id, usos), ' | ' ORDER BY (old_id = canonical_id) DESC, old_id) AS grafias
FROM catalog_map
WHERE catalog = 'models'
GROUP BY canonical_id
ORDER BY random()
LIMIT 25;

-- Alvo de cada linha de brands_models_types e sobrevivente por tripla (menor id).
CREATE TEMP TABLE bmt_plan ON COMMIT DROP AS
SELECT b.id,
       coalesce(mt.canonical_id, b.id_type)   AS new_type,
       coalesce(mb.canonical_id, b.id_brand)  AS new_brand,
       coalesce(mm.canonical_id, b.id_model)  AS new_model,
       min(b.id) OVER (PARTITION BY coalesce(mt.canonical_id, b.id_type),
                                    coalesce(mb.canonical_id, b.id_brand),
                                    coalesce(mm.canonical_id, b.id_model)) AS survivor_id
FROM brands_models_types b
LEFT JOIN catalog_map mt ON mt.catalog = 'types'  AND mt.old_id = b.id_type
LEFT JOIN catalog_map mb ON mb.catalog = 'brands' AND mb.old_id = b.id_brand
LEFT JOIN catalog_map mm ON mm.catalog = 'models' AND mm.old_id = b.id_model;

-- 1. Reaponta devices das combinacoes que serao fundidas.
UPDATE devices d
SET id_brand_model_type = p.survivor_id
FROM bmt_plan p
WHERE d.id_brand_model_type = p.id AND p.id <> p.survivor_id;

-- 2. Apaga as combinacoes absorvidas (antes do UPDATE, por causa do indice unico da V8).
DELETE FROM brands_models_types b
USING bmt_plan p
WHERE b.id = p.id AND p.id <> p.survivor_id;

-- 3. Reaponta as combinacoes sobreviventes para os canonicos.
UPDATE brands_models_types b
SET id_type = p.new_type, id_brand = p.new_brand, id_model = p.new_model
FROM bmt_plan p
WHERE b.id = p.id AND p.id = p.survivor_id
  AND (b.id_type, b.id_brand, b.id_model) IS DISTINCT FROM (p.new_type, p.new_brand, p.new_model);

-- 4. Apaga os cadastros repetidos (ja sem referencias).
DELETE FROM types  t USING catalog_map m WHERE m.catalog = 'types'  AND m.old_id = t.id AND m.old_id <> m.canonical_id;
DELETE FROM brands x USING catalog_map m WHERE m.catalog = 'brands' AND m.old_id = x.id AND m.old_id <> m.canonical_id;
DELETE FROM models y USING catalog_map m WHERE m.catalog = 'models' AND m.old_id = y.id AND m.old_id <> m.canonical_id;

\echo
\echo '== DEPOIS: contagens (devices e devices_com_catalogo nao podem mudar) =='
SELECT (SELECT count(*) FROM types)               AS types,
       (SELECT count(*) FROM brands)              AS brands,
       (SELECT count(*) FROM models)              AS models,
       (SELECT count(*) FROM brands_models_types) AS brands_models_types,
       (SELECT count(*) FROM devices)             AS devices,
       (SELECT count(*) FROM devices WHERE id_brand_model_type IS NOT NULL) AS devices_com_catalogo;

\echo
\echo '== DEPOIS: grupos restantes com a mesma chave (esperado: 0 linhas) =='
SELECT 'types' AS catalog, regexp_replace(lower(public.immutable_unaccent(type)), '[^a-z0-9]', '', 'g') AS key, count(*)
FROM types GROUP BY 2 HAVING count(*) > 1
UNION ALL
SELECT 'brands', regexp_replace(lower(public.immutable_unaccent(brand)), '[^a-z0-9]', '', 'g'), count(*)
FROM brands GROUP BY 2 HAVING count(*) > 1
UNION ALL
SELECT 'models', regexp_replace(lower(public.immutable_unaccent(model)), '[^a-z0-9]', '', 'g'), count(*)
FROM models GROUP BY 2 HAVING count(*) > 1;

\echo
\echo '== DEPOIS: orfaos (esperado: 0 e 0) =='
SELECT (SELECT count(*) FROM devices d WHERE d.id_brand_model_type IS NOT NULL
          AND NOT EXISTS (SELECT 1 FROM brands_models_types b WHERE b.id = d.id_brand_model_type)) AS devices_orfaos,
       (SELECT count(*) FROM brands_models_types b
         WHERE NOT EXISTS (SELECT 1 FROM types t WHERE t.id = b.id_type)
            OR NOT EXISTS (SELECT 1 FROM brands x WHERE x.id = b.id_brand)
            OR NOT EXISTS (SELECT 1 FROM models y WHERE y.id = b.id_model)) AS combinacoes_orfas;

-- ENSAIO: troque ROLLBACK por COMMIT depois de revisar a saida acima.
ROLLBACK;
