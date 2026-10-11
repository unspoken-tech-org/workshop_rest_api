-- Fix duplicates before applying UNIQUE constraints
-- This migration normalizes and removes duplicates from colors, types, brands, and models
-- Updates all references before removing duplicate records

BEGIN;

-- ============================================================================
-- PART 1: COLORS
-- ============================================================================

-- Normalize colors: TRIM and LOWER
UPDATE colors
SET color = LOWER(TRIM(color))
WHERE color <> LOWER(TRIM(color));

-- Create temporary table for mapping duplicate colors
CREATE TEMP TABLE color_mapping AS
WITH duplicates AS (
    -- Find groups of duplicate normalized colors
    SELECT 
        LOWER(TRIM(color)) as color_lower,
        MIN(id) as keep_id,  -- Oldest ID that will be kept
        ARRAY_AGG(id ORDER BY id) as all_ids  -- All group IDs
    FROM colors 
    GROUP BY LOWER(TRIM(color))
    HAVING COUNT(*) > 1
),
mapping_data AS (
    -- Create the mapping: each duplicated ID -> ID to be kept
    SELECT 
        unnest(all_ids) as old_id,
        keep_id as new_id,
        color_lower
    FROM duplicates
)
SELECT 
    old_id,
    new_id,
    color_lower,
    CASE WHEN old_id = new_id THEN true ELSE false END as is_keeper
FROM mapping_data;

-- Update COLORS references in devices.color_ids (arrays)
-- Substitute duplicate IDs with the correct IDs in the arrays and remove duplicates inside the array
UPDATE devices d
SET color_ids = (
    SELECT ARRAY_AGG(DISTINCT color_id ORDER BY color_id)
    FROM (
        SELECT COALESCE(cm.new_id, color_id) as color_id
        FROM unnest(d.color_ids) AS color_id
        LEFT JOIN color_mapping cm ON cm.old_id = color_id AND NOT cm.is_keeper
    ) AS updated_ids
)
WHERE EXISTS (
    SELECT 1
    FROM unnest(d.color_ids) AS color_id
    INNER JOIN color_mapping cm ON cm.old_id = color_id AND NOT cm.is_keeper
);

-- Remove duplicate COLORS (keep only the oldest in each group)
DELETE FROM colors 
WHERE id IN (
    SELECT old_id 
    FROM color_mapping 
    WHERE NOT is_keeper
);

-- Reset colors sequence
SELECT setval(pg_get_serial_sequence('colors', 'id'), coalesce(MAX(id), 1)) FROM colors;

-- Clean up the temporary colors table
DROP TABLE IF EXISTS color_mapping;

-- ============================================================================
-- PART 2: TYPES (check and fix if new duplicates exist)
-- ============================================================================

-- Normalize types: TRIM and LOWER
UPDATE types
SET type = LOWER(TRIM(REGEXP_REPLACE(type, '\\s+', ' ', 'g')))
WHERE type <> LOWER(TRIM(REGEXP_REPLACE(type, '\\s+', ' ', 'g')));

-- Create temporary table for mapping duplicate types
CREATE TEMP TABLE type_mapping AS
WITH duplicates AS (
    SELECT 
        REGEXP_REPLACE(TRIM(LOWER(type)), '\\s+', ' ', 'g') as type_lower,
        MIN(id) as keep_id,
        ARRAY_AGG(id ORDER BY id) as all_ids
    FROM types 
    GROUP BY REGEXP_REPLACE(TRIM(LOWER(type)), '\\s+', ' ', 'g')
    HAVING COUNT(*) > 1
),
mapping_data AS (
    SELECT 
        unnest(all_ids) as old_id,
        keep_id as new_id,
        type_lower
    FROM duplicates
)
SELECT 
    old_id,
    new_id,
    type_lower,
    CASE WHEN old_id = new_id THEN true ELSE false END as is_keeper
FROM mapping_data;

-- Update TYPES references in brands_models_types
UPDATE brands_models_types 
SET id_type = tm.new_id
FROM type_mapping tm
WHERE brands_models_types.id_type = tm.old_id 
  AND NOT tm.is_keeper;

-- Remove duplicate TYPES
DELETE FROM types 
WHERE id IN (
    SELECT old_id 
    FROM type_mapping 
    WHERE NOT is_keeper
);

-- Reset types sequence
SELECT setval(pg_get_serial_sequence('types', 'id'), coalesce(MAX(id), 1)) FROM types;

DROP TABLE IF EXISTS type_mapping;

-- ============================================================================
-- PART 3: BRANDS (check and fix if new duplicates exist)
-- ============================================================================

-- Normalize brands: TRIM and LOWER
UPDATE brands
SET brand = LOWER(TRIM(REGEXP_REPLACE(brand, '\\s+', ' ', 'g')))
WHERE brand <> LOWER(TRIM(REGEXP_REPLACE(brand, '\\s+', ' ', 'g')));

-- Create temporary table for mapping duplicate brands
CREATE TEMP TABLE brand_mapping AS
WITH duplicates AS (
    SELECT 
        REGEXP_REPLACE(TRIM(LOWER(brand)), '\\s+', ' ', 'g') as brand_lower,
        MIN(id) as keep_id,
        ARRAY_AGG(id ORDER BY id) as all_ids
    FROM brands 
    GROUP BY REGEXP_REPLACE(TRIM(LOWER(brand)), '\\s+', ' ', 'g')
    HAVING COUNT(*) > 1
),
mapping_data AS (
    SELECT 
        unnest(all_ids) as old_id,
        keep_id as new_id,
        brand_lower
    FROM duplicates
)
SELECT 
    old_id,
    new_id,
    brand_lower,
    CASE WHEN old_id = new_id THEN true ELSE false END as is_keeper
FROM mapping_data;

-- Update BRANDS references in brands_models_types
UPDATE brands_models_types 
SET id_brand = bm.new_id
FROM brand_mapping bm
WHERE brands_models_types.id_brand = bm.old_id 
  AND NOT bm.is_keeper;

-- Remove duplicate BRANDS
DELETE FROM brands 
WHERE id IN (
    SELECT old_id 
    FROM brand_mapping 
    WHERE NOT is_keeper
);

-- Reset brands sequence
SELECT setval(pg_get_serial_sequence('brands', 'id'), coalesce(MAX(id), 1)) FROM brands;

DROP TABLE IF EXISTS brand_mapping;

-- ============================================================================
-- PART 4: MODELS (check and fix if new duplicates exist)
-- ============================================================================

-- Normalize models: TRIM and LOWER
UPDATE models
SET model = LOWER(TRIM(REGEXP_REPLACE(model, '\\s+', ' ', 'g')))
WHERE model <> LOWER(TRIM(REGEXP_REPLACE(model, '\\s+', ' ', 'g')));

-- Create temporary table for mapping duplicate models
CREATE TEMP TABLE model_mapping AS
WITH duplicates AS (
    SELECT 
        REGEXP_REPLACE(TRIM(LOWER(model)), '\\s+', ' ', 'g') as model_lower,
        MIN(id) as keep_id,
        ARRAY_AGG(id ORDER BY id) as all_ids
    FROM models 
    GROUP BY REGEXP_REPLACE(TRIM(LOWER(model)), '\\s+', ' ', 'g')
    HAVING COUNT(*) > 1
),
mapping_data AS (
    SELECT 
        unnest(all_ids) as old_id,
        keep_id as new_id,
        model_lower
    FROM duplicates
)
SELECT 
    old_id,
    new_id,
    model_lower,
    CASE WHEN old_id = new_id THEN true ELSE false END as is_keeper
FROM mapping_data;

-- Update MODELS references in brands_models_types
UPDATE brands_models_types 
SET id_model = mm.new_id
FROM model_mapping mm
WHERE brands_models_types.id_model = mm.old_id 
  AND NOT mm.is_keeper;

-- Remove duplicate MODELS
DELETE FROM models 
WHERE id IN (
    SELECT old_id 
    FROM model_mapping 
    WHERE NOT is_keeper
);

-- Reset models sequence
SELECT setval(pg_get_serial_sequence('models', 'id'), coalesce(MAX(id), 1)) FROM models;

DROP TABLE IF EXISTS model_mapping;

COMMIT;

