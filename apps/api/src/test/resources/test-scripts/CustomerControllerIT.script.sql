INSERT INTO customers (id, name, created_at, cpf, gender, email)
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
    ),
    (
    3,
    'Maria Oliveira',
    '2023-01-06 14:41:00.968173',
    '98765432100',
    'feminino',
    'maria@gmail.com'
    ),
    (
    4,
    'João Cláudio da Silva',
    '2025-01-06 14:41:00.968173',
    '48541376060',
    'masculino',
    'joao@gmail.com'
    );

-- Insert unique phones into the phones table
INSERT INTO phones (id, number)
VALUES
    (1, '4430356678'),
    (2, '44988098766'),
    (3, '44988255540'),
    (4, '99988887777'),
    (5, '48541376060');

-- Create associations in the customer_phones table
INSERT INTO customer_phones (id_customer, id_phone, is_main, alias)
VALUES
    (1, 1, true, 'Celular Principal'),   -- Alfonso: 4430356678 as primary
    (2, 2, false, 'Trabalho'),           -- Lucas: 44988098766 as secondary
    (2, 3, true, 'Luiz'),                -- Lucas: 44988255540 as primary
    (3, 4, true, 'Telefone Compartilhado'), -- Maria: 99988887777 as primary
    (4, 5, true, 'Celular Principal');   -- João: 48541376060 as primary