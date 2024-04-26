INSERT INTO customers(id, name, insert_date, cpf, gender, email, phone, whatsapp) VALUES
 (
    1,
    'Alfonso Zimmer',
    '2021-04-26 08:41:00.968173',
    '31781477051',
    'm',
    'zimmer@gmail.com',
    '4430356678',
    '44988254493'
    ),
    (
    2,
    'Lucas Ribeiro Naga',
    '2023-01-06 14:41:00.968173',
    '46203912042',
    'm',
    'zimmer@gmail.com',
    '44988255540',
    NULL
    );
    
INSERT INTO device_status(id, status) 
VALUES 
(1, 'ACTIVE'),
(2, 'INACTIVE');

INSERT INTO brands(id, brand) 
VALUES
(1, 'Brand 1'),
(2, 'Brand 2');

INSERT INTO models (id, model) 
VALUES
(1, 'Model 1'),
(2, 'Model 2');

INSERT INTO technicians(id, technician, number) 
VALUES
(1, 'Technician 1', '44988254493'),
(2, 'Technician 2', '44988254493');

INSERT INTO types(id, type) 
VALUES
(1, 'Type 1'),
(2, 'Type 2');

INSERT INTO brands_models(id, id_brand, id_model)
VALUES
(1, 1, 1),
(2, 2, 2);

INSERT INTO brands_models_types(id, id_brand_model, id_type)
VALUES
(1, 1, 1),
(2, 2, 2)
;

INSERT INTO devices (id, id_customer, id_brand_model_type, id_device_status, id_technician, entry_date, departure_date, problem, observation, budget, labor_value, has_urgency, last_update)
VALUES
(1, 1,  1, 1, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 1', 'Observation 1', 'Budget 1', 100.00, true, '2021-04-26 08:41:00.968173'),
(2, 2,  2, 2, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 2', 'Observation 2', 'Budget 2', 200.00, false, '2023-01-06 14:41:00.968173');

--BEGIN;
SELECT setval(pg_get_serial_sequence('devices', 'id'), coalesce(MAX(id), 1)) from devices;
--COMMIT;
--ALTER TABLE devices ALTER COLUMN id RESTART WITH 3;


