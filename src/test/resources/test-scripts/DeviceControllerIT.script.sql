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
),
(
3,
'Francisco Alves Beltrão',
'2023-01-06 14:41:00.968173',
'09857367410',
'm',
'belt@gmail.com',
'44988252235',
NULL
)
;

INSERT INTO technicians(id, technician, number)
VALUES
(1, 'Technician 1', '44988254493'),
(2, 'Technician 2', '44988254493')
;
    
INSERT INTO device_status(id, status) 
VALUES 
(1, 'novo'),
(2, 'em_andamento'),
(3, 'aguardando'),
(4, 'entregue'),
(5, 'descartado')
;

INSERT INTO brands(id, brand) 
VALUES
(1, 'Arno'),
(2, 'Wallita'),
(3, 'Britania'),
(4, 'Philco'),
(5, 'Electrolux')
;

INSERT INTO models (id, model) 
VALUES
(1, 'Model 1'),
(2, 'Model 2'),
(3, 'Model 3'),
(4, 'Model 4'),
(5, 'Model 5');

INSERT INTO types(id, type) 
VALUES
(1, 'Ventilador'),
(2, 'Liquidificador'),
(3, 'Batedeira'),
(4, 'Ferro de passar'),
(5, 'Aspirador')
;

INSERT INTO brands_models_types(id, id_brand, id_model, id_type)
VALUES
(1, 1, 1, 1),
(2, 2, 2, 2),
(3, 3, 3, 3),
(4, 4, 4, 4),
(5, 5, 5, 5)
;

INSERT INTO colors(id, color) VALUES
(1, 'preto'),
(2, 'cinza'),
(3, 'branco'),
(4, 'azul'),
(5, 'amarelo'),
(6, 'roxo'),
(7, 'verde')
;


INSERT INTO devices (id, id_customer, id_brand_model_type, id_device_status, id_technician, entry_date, departure_date, problem, observation, budget, labor_value, has_urgency, is_revision, last_update, color_ids)
VALUES
(1, 1, 1, 1, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 1', 'Observation 1', 'Budget 1', 100.00, true, false, null, ('{1}')),
(2, 2, 2, 2, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 2', 'Observation 2', 'Budget 2', 200.00, false, true, '2023-01-06 14:41:00.968173',('{2}')),
(3, 1, 3, 3, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 3', 'Observation 3', 'Budget 3', 300.00, true, false, null, ('{3}')),
(4, 2, 4, 4, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 4', 'Observation 4', 'Budget 4', 400.00, false, true, '2023-01-06 14:41:00.968173', ('{4}')),
(5, 1, 5, 5, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 5', 'Observation 5', 'Budget 5', 500.00, true, false, null, ('{1,3}')),
(6, 2, 1, 1, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 6', 'Observation 6', 'Budget 6', 600.00, false, true, '2023-01-06 14:41:00.968173', ('{5}')),
(7, 1, 2, 2, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 7', 'Observation 7', 'Budget 7', 700.00, true, false, null, ('{5,6}')),
(8, 2, 3, 3, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 8', 'Observation 8', 'Budget 8', 800.00, false, true, '2023-01-06 14:41:00.968173', ('{1}')),
(9, 1, 4, 4, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 9', 'Observation 9', 'Budget 9', 900.00, true, false, '2021-04-26 08:41:00.968173', ('{2}')),
(10, 2, 5, 5, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 10', 'Observation 10', 'Budget 10', 1000.00, false, true, '2023-01-06 14:41:00.968173', ('{3}')),
(11, 1, 1, 1, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 11', 'Observation 11', 'Budget 11', 1100.00, true, false, '2021-04-26 08:41:00.968173', ('{5}')),
(12, 2, 2, 2, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 12', 'Observation 12', 'Budget 12', 1200.00, false, true, '2023-01-06 14:41:00.968173', ('{3}')),
(13, 1, 3, 3, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 13', 'Observation 13', 'Budget 13', 1300.00, true, false, '2021-04-26 08:41:00.968173', ('{2}')),
(14, 2, 4, 4, 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 14', 'Observation 14', 'Budget 14', 1400.00, false, true, '2023-01-06 14:41:00.968173', ('{1}')),
(15, 1, 5, 5, 1, '2021-04-26 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 15', 'Observation 15', 'Budget 15', 1500.00, true, false, '2021-04-26 08:41:00.968173', ('{3, 2}'))
;

--BEGIN;
SELECT setval(pg_get_serial_sequence('devices', 'id'), coalesce(MAX(id), 1)) from devices;
--COMMIT;
--ALTER TABLE devices ALTER COLUMN id RESTART WITH 3;


