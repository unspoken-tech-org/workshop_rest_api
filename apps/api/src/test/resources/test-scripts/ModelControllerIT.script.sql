-- ModelControllerIT.script.sql
-- Seed types, brands, and models for fuzzy search tests

INSERT INTO types (id, type, created_at, updated_at) VALUES
    (1, 'Lavadora', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Micro-ondas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO brands (id, brand, created_at, updated_at) VALUES
    (1, 'Samsung', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'LG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Panasonic', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO models (id, model, created_at, updated_at) VALUES
    (1, 'WT12345', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'WT12300', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'WT500X', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'SM-L22', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'SM-G991', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'NN-ST25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'NN-SD45', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, 'MS-TRL52', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Brand 1 (Samsung) + Type 1 (Lavadora): WT12345, WT12300, WT500X
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 1, 2, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 1, 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Brand 1 (Samsung) + Type 2 (Micro-ondas): SM-L22, SM-G991
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (4, 1, 4, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 1, 5, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Brand 2 (LG) + Type 1 (Lavadora): WT12345
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (6, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Brand 3 (Panasonic) + Type 2 (Micro-ondas): NN-ST25, NN-SD45, MS-TRL52
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (7, 3, 6, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, 3, 7, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9, 3, 8, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
