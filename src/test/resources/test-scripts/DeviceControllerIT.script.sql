INSERT INTO customers(id, name, insert_date, cpf, gender, email) VALUES
(
1,
'Alfonso Zimmer',
'2021-04-26 08:41:00.968173',
'31781477051',
'masculino',
'zimmer@gmail.com'
),
(
2,
'Lucas Ribeiro Naga',
'2023-01-06 14:41:00.968173',
'46203912042',
'masculino',
'zimmer@gmail.com'
),
(
3,
'Francisco Alves Beltrão',
'2023-01-06 14:41:00.968173',
'09857367410',
'masculino',
'belt@gmail.com'
)
;

INSERT INTO technicians(id, technician_name, number)
VALUES
(1, 'Technician 1', '44988254493'),
(2, 'Technician 2', '44988254493')
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

INSERT INTO devices (id, id_customer, id_brand_model_type, device_status, id_technician, entry_date, departure_date, problem, observation, budget, labor_value, service_value, has_urgency, is_revision, last_update, color_ids)
VALUES
(1, 1, 1, 'NOVO', 1, '2023-04-10 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 1', 'Observation 1', 'Budget 1', 50.00, 100.00, true, false, null, ('{1}')),
(2, 2, 2, 'EM_ANDAMENTO', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 2', 'Observation 2', 'Budget 2', 50.00, 200.00, false, true, '2023-01-06 14:41:00.968173',('{2}')),
(3, 1, 3, 'AGUARDANDO', 1, '2021-09-03 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 3', 'Observation 3', 'Budget 3', 50.00, 300.00, true, false, null, ('{3}')),
(4, 2, 4, 'ENTREGUE', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 4', 'Observation 4', 'Budget 4', 50.00, 400.00, false, true, '2023-01-06 14:41:00.968173', ('{4}')),
(5, 1, 5, 'DESCARTADO', 1, '2017-04-28 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 5', 'Observation 5', 'Budget 5', 50.00, 500.00, true, false, null, ('{1,3}')),
(6, 2, 1, 'EM_ANDAMENTO', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 6', 'Observation 6', 'Budget 6', 50.00, 600.00, false, true, '2023-01-06 14:41:00.968173', ('{5}')),
(7, 1, 2, 'EM_ANDAMENTO', 1, '2020-10-10 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 7', 'Observation 7', 'Budget 7', 50.00, 700.00, true, false, null, ('{5,6}')),
(8, 2, 3,  'AGUARDANDO', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 8', 'Observation 8', 'Budget 8', 50.00, 800.00, false, true, '2023-01-06 14:41:00.968173', ('{1}')),
(9, 1, 4, 'ENTREGUE', 1, '2025-02-04 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 9', 'Observation 9', 'Budget 9', 50.00, 900.00, true, false, '2021-04-26 08:41:00.968173', ('{2}')),
(10, 2, 5,  'DESCARTADO', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 10', 'Observation 10', 'Budget 10', 50.00, 1000.00, false, true, '2023-01-06 14:41:00.968173', ('{3}')),
(11, 1, 1, 'NOVO', 1, '2021-04-01 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 11', 'Observation 11', 'Budget 11', 50.00, 1100.00, true, false, '2021-04-26 08:41:00.968173', ('{5}')),
(12, 2, 2, 'EM_ANDAMENTO', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 12', 'Observation 12', 'Budget 12', 50.00, 1200.00, false, true, '2023-01-06 14:41:00.968173', ('{3}')),
(13, 1, 3, 'AGUARDANDO', 1, '2021-04-02 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 13', 'Observation 13', 'Budget 13', 50.00, 1300.00, true, false, '2021-04-26 08:41:00.968173', ('{2}')),
(14, 2, 4, 'ENTREGUE', 2, '2023-01-06 14:41:00.968173', '2023-01-06 14:41:00.968173', 'Problem 14', 'Observation 14', 'Budget 14', 50.00, 1400.00, false, true, '2023-01-06 14:41:00.968173', ('{1}')),
(15, 1, 5, 'DESCARTADO', 1, '2021-04-22 08:41:00.968173', '2021-04-26 08:41:00.968173', 'Problem 15', 'Observation 15', 'Budget 15', 50.00, 1500.00, true, false, '2021-04-26 08:41:00.968173', ('{3, 2}'))
;

INSERT INTO phones(id, number, alias)
VALUES
(1, '4430356678', 'Telefone Secundário'),
(2, '44988254493', 'Telefone Principal'),
(3, '44988255540', 'Lucas Principal'),
(4, '44988252235', 'Francisco Principal');

INSERT INTO customer_phones(id_customer, id_phone, is_main)
VALUES
(1, 1, false),
(1, 2, true),
(2, 3, true),
(3, 4, true);

INSERT INTO customer_contact(id, id_device, id_technician, phone, device_status, type, has_made_contact, last_contact, conversation)
VALUES
(1, 1, 1, '44988254493', 'AGUARDANDO', 'mensagem', false, '2025-01-06 14:41:00.968173', 'Foi passado o orçamento e aguardo resposta'),
(2, 1, 1, null, 'EM_ANDAMENTO', 'pessoalmente', true, '2025-01-07 14:41:00.968173', 'Cliente aceitou o orçamento'),
(3, 1, 1, '44988254493', 'PRONTO', 'mensagem', true, '2025-01-08 14:41:00.968173', 'Avisado que o aparelho está pronto')
;

INSERT INTO payments(id, id_device, payment_date, payment_type, payment_value, category)
VALUES
(1, 1, '2023-04-22 08:41:00.968173', 'credito', 100, 'parcial'),
(2, 1, '2023-04-29 08:41:00.968173', 'credito', 100, 'parcial')
;


