INSERT INTO customers (id, name, insert_date, cpf, gender, email)
VALUES
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
    'lucas@gmail.com'
    );

INSERT INTO phones (id_customer, name, number, is_main)
VALUES
    (1, null, '4430356678', true),
    (2, 'Luiz', '44988255540', false);