-- Constraint UNIQUE case-insensitive para colors
CREATE UNIQUE INDEX IF NOT EXISTS idx_colors_color_unique_lower 
ON colors (LOWER(TRIM(color)));

-- Constraint UNIQUE case-insensitive para types
CREATE UNIQUE INDEX IF NOT EXISTS idx_types_type_unique_lower 
ON types (LOWER(TRIM(type)));

-- Constraint UNIQUE case-insensitive para brands
CREATE UNIQUE INDEX IF NOT EXISTS idx_brands_brand_unique_lower 
ON brands (LOWER(TRIM(brand)));

-- Constraint UNIQUE case-insensitive para models
CREATE UNIQUE INDEX IF NOT EXISTS idx_models_model_unique_lower 
ON models (LOWER(TRIM(model)));

