-- BrandControllerIT.script.sql
-- Seed types, brands, and models for fuzzy search tests

INSERT INTO models (id, model, created_at, updated_at) VALUES
    (1, 'Generic Model', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO types (id, type, created_at, updated_at) VALUES
    (1, 'Lavadora', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'Lava e Seca', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'Micro-ondas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Refrigerador', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO brands (id, brand, created_at, updated_at) VALUES
    (1, 'Samsung', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'LG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'LG Pro', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 'Brastemp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'Electrolux', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 'Consul', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 'Panasonic', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (8, 'Midea', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Type 1 (Lavadora): Samsung, LG, Brastemp, Electrolux
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (1, 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 2, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 5, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Type 2 (Lava e Seca): Samsung, LG, LG Pro
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (5, 1, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (6, 2, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7, 3, 1, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Type 3 (Micro-ondas): Panasonic, Midea, Consul
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (8, 7, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9, 8, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (10, 6, 1, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Type 4 (Refrigerador): Samsung, Electrolux
INSERT INTO brands_models_types (id, id_brand, id_model, id_type, created_at, updated_at) VALUES
    (11, 1, 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (12, 5, 1, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
