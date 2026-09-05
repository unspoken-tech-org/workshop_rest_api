CREATE UNIQUE INDEX IF NOT EXISTS idx_brands_models_types_unique
ON brands_models_types (id_brand, id_model, id_type);

CREATE UNIQUE INDEX IF NOT EXISTS idx_technicians_name_unique_lower
ON technicians (LOWER(TRIM(technician_name)));
