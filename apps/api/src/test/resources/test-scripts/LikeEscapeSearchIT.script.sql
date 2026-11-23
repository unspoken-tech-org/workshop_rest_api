-- Names, phones and catalog entries with LIKE metacharacters (%, _, \).
-- standard_conforming_strings is on: '\' below is a literal backslash.
INSERT INTO customers (id, name, created_at, cpf, gender, email)
VALUES
    (1, 'Loja 100% Eletrica', '2024-01-01 10:00:00', '11144477735', 'masculino', 'loja100@example.com'),
    (2, 'Loja 1000 Eletrica', '2024-01-02 10:00:00', '22233344405', 'masculino', 'loja1000@example.com'),
    (3, 'Oficina a_b', '2024-01-03 10:00:00', '33366699912', 'feminino', 'a_b@example.com'),
    (4, 'Oficina axb', '2024-01-04 10:00:00', '44455566607', 'feminino', 'axb@example.com'),
    (5, 'Barra \ Invertida', '2024-01-05 10:00:00', '55588899903', 'masculino', 'barra@example.com'),
    (6, 'José Ávila', '2024-01-06 10:00:00', '66611122204', 'masculino', 'jose@example.com');

INSERT INTO phones (id, number)
VALUES
    (1, '44911110001'),
    (2, '44911110002'),
    (3, '44911110003'),
    (4, '44911110004'),
    (5, '44911110005'),
    (6, '44911110006');

INSERT INTO customer_phones (id_customer, id_phone, is_main, alias)
VALUES
    (1, 1, true, 'Principal'),
    (2, 2, true, 'Principal'),
    (3, 3, true, 'Principal'),
    (4, 4, true, 'Principal'),
    (5, 5, true, 'Principal'),
    (6, 6, true, 'Principal');

INSERT INTO technicians (id, technician_name, number)
VALUES (1, 'Technician 1', '44988254493');

INSERT INTO brands (id, brand) VALUES (1, 'Arno'), (2, 'Britânia');
INSERT INTO models (id, model) VALUES (1, 'Turbo 100%'), (2, 'Turbo 1000');
INSERT INTO types (id, type) VALUES (1, 'Ventilador'), (2, 'Liquidificador');

INSERT INTO brands_models_types (id, id_brand, id_model, id_type)
VALUES (1, 1, 1, 1), (2, 2, 2, 2);

INSERT INTO colors (id, color) VALUES (1, 'preto');

INSERT INTO devices (id, id_customer, id_brand_model_type, device_status, id_technician, entry_date, problem, budget_fee, has_urgency, is_revision, color_ids)
VALUES
    (1, 1, 1, 'NOVO', 1, '2024-02-01 10:00:00', 'Problem 1', 0.00, false, false, '{1}'),
    (2, 2, 2, 'NOVO', 1, '2024-02-02 10:00:00', 'Problem 2', 0.00, false, false, '{1}'),
    (3, 3, 1, 'NOVO', 1, '2024-02-03 10:00:00', 'Problem 3', 0.00, false, false, '{1}'),
    (4, 4, 2, 'NOVO', 1, '2024-02-04 10:00:00', 'Problem 4', 0.00, false, false, '{1}'),
    (5, 5, 1, 'NOVO', 1, '2024-02-05 10:00:00', 'Problem 5', 0.00, false, false, '{1}'),
    (6, 6, 2, 'NOVO', 1, '2024-02-06 10:00:00', 'Problem 6', 0.00, false, false, '{1}');
