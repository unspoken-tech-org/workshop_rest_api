-- =============================================
-- TIPOS (type variations for fuzzy testing)
-- =============================================
INSERT INTO types(id, type) VALUES
(6, 'Micro-ondas'),             -- base
(7, 'Micro-ondas de embutir'),  -- variação longa
(8, 'Microondas'),              -- sem hífen (≠ base)
(9, 'Ventilador'),              -- tipo diferente
(10, 'Ventilador de mesa'),     -- subtipo similar ao "Ventilador"
(11, 'Liquidificador'),         -- tipo novo
(12, 'Air Fryer'),              -- variação de "air frayer" no banco real
(13, 'Aspirador robô'),         -- prefixo "aspirador" compartilhado
(14, 'Ferro a vapor'),          -- prefixo "ferro" compartilhado
(15, 'Chaleira elétrica'),      -- variação de acento
(16, 'Lava louças'),            -- variação de "lava e seca"
(17, 'Ventilador de coluna'),   -- prefixo "ventilador" compartilhado
(18, 'Secadora');                -- tipo novo

-- =============================================
-- MARCAS (brand variations including typos)
-- =============================================
INSERT INTO brands(id, brand) VALUES
(6, 'Samsung'),     -- marca correta
(7, 'Sansung'),     -- typo real (ocorre no banco de produção)
(8, 'Britânia'),    -- com acento
(9, 'Britania'),    -- sem acento (ocorre no banco de produção)
(10, 'Arno'),       -- marca existente
(11, 'Wallita'),    -- marca existente
(12, 'Brastemp'),   -- marca real
(13, 'Electrolux'), -- marca real
(14, 'Consul'),     -- marca real
(15, 'Brastenp'),   -- typo de Brastemp
(16, 'Eletrolux'),  -- typo de Electrolux
(17, 'Consusl'),    -- typo de Consul
(18, 'Panasonic'),  -- marca real
(19, 'Panassonic'), -- typo de Panasonic
(20, 'Mondial');    -- marca real

-- =============================================
-- MODELOS (realistic names)
-- =============================================
INSERT INTO models(id, model) VALUES
(6, 'Smart Cook'),
(7, 'Mega Plus'),
(8, 'Turbo 3000'),
(9, 'Easy Breeze'),
(10, 'Silent Pro'),
(11, 'Power Mix'),
(12, 'Air Flow'),
(13, 'Power Flow'),
(14, 'Smart Breeze'),
(15, 'Turbo Eco'),
(16, 'Easy Flow'),
(17, 'Mega Pro'),
(18, 'Silent Air');

-- =============================================
-- CLIENTES (nomes variados para fuzzy)
-- =============================================
INSERT INTO customers(id, name, insert_date, cpf, gender, email) VALUES
(10, 'João Silva',                '2024-01-01 00:00:00', '11111111111', 'masculino', 'joao@test.com'),
(11, 'Maria Aparecida',           '2024-01-02 00:00:00', '22222222222', 'feminino',  'maria@test.com'),
(12, 'Pedro Oliveira',            '2024-01-03 00:00:00', '33333333333', 'masculino', 'pedro@test.com'),
(13, 'Adilson Ferreira da Silva', '2024-01-04 00:00:00', '44444444444', 'masculino', 'adilson@test.com'),
(14, 'Alessandro de Paula',       '2024-01-05 00:00:00', '55555555555', 'masculino', 'alessandro@test.com'),
(15, 'Maria Aparecida de Oliveira', '2024-01-06 00:00:00', '66666666666', 'feminino', 'mariaaparecida@test.com'),
(16, 'José Carlos da Silva',      '2024-01-07 00:00:00', '77777777777', 'masculino', 'josecarlos@test.com'),
(17, 'Ana Beatriz Souza',         '2024-01-08 00:00:00', '88888888888', 'feminino',  'anabeatriz@test.com'),
(18, 'Paulo Henrique Santos',     '2024-01-09 00:00:00', '99999999999', 'masculino', 'paulohenrique@test.com'),
(19, 'João Pedro da Silva',       '2024-01-10 00:00:00', '10101010101', 'masculino', 'joaopedro@test.com'),
(20, 'João Paulo Ferreira',       '2024-01-11 00:00:00', '11111111112', 'masculino', 'joaopaulo@test.com'),
(21, 'João Victor Almeida Santos','2024-01-12 00:00:00', '12121212123', 'masculino', 'joaovictor@test.com');

-- =============================================
-- BRAND_MODEL_TYPE combos
-- =============================================
INSERT INTO brands_models_types(id, id_brand, id_model, id_type) VALUES
-- Combos originais
(10, 6, 6, 6),    -- Samsung + Smart Cook  + Micro-ondas
(11, 7, 6, 6),    -- Sansung + Smart Cook  + Micro-ondas (typo brand)
(12, 8, 7, 6),    -- Britânia + Mega Plus  + Micro-ondas
(13, 9, 7, 6),    -- Britania + Mega Plus  + Micro-ondas (no accent)
(14, 6, 8, 7),    -- Samsung + Turbo 3000  + Micro-ondas de embutir
(15, 8, 9, 9),    -- Britânia + Easy Breeze + Ventilador
(16, 8, 10, 10),  -- Britânia + Silent Pro  + Ventilador de mesa
(17, 6, 11, 11),  -- Samsung + Power Mix + Liquidificador
(18, 10, 11, 11), -- Arno + Power Mix + Liquidificador
(19, 11, 12, 9),  -- Wallita + Air Flow + Ventilador
(20, 6, 9, 9),    -- Samsung + Easy Breeze + Ventilador
(21, 8, 6, 6),    -- Britânia + Smart Cook + Micro-ondas
-- Novos combos: typos de marca
(22, 12, 6, 6),   -- Brastemp + Smart Cook + Micro-ondas
(23, 13, 7, 6),   -- Electrolux + Mega Plus + Micro-ondas
(24, 14, 8, 9),   -- Consul + Turbo 3000 + Ventilador
(25, 15, 6, 6),   -- Brastenp + Smart Cook + Micro-ondas (typo)
(26, 16, 7, 6),   -- Eletrolux + Mega Plus + Micro-ondas (typo)
(27, 17, 8, 9),   -- Consusl + Turbo 3000 + Ventilador (typo)
(28, 18, 11, 11), -- Panasonic + Power Mix + Liquidificador
(29, 19, 11, 11), -- Panassonic + Power Mix + Liquidificador (typo)
(30, 20, 12, 14), -- Mondial + Air Flow + Ferro a vapor
-- Novos combos: tipos novos
(31, 6, 10, 13),  -- Samsung + Silent Pro + Aspirador robô
(32, 8, 12, 15),  -- Britânia + Air Flow + Chaleira elétrica
(33, 10, 13, 16), -- Arno + Power Flow + Lava louças
(34, 11, 14, 17), -- Wallita + Smart Breeze + Ventilador de coluna
(35, 13, 15, 18), -- Electrolux + Turbo Eco + Secadora
(36, 12, 16, 13), -- Brastemp + Easy Flow + Aspirador robô
(37, 14, 17, 14), -- Consul + Mega Pro + Ferro a vapor
(38, 18, 18, 15); -- Panasonic + Silent Air + Chaleira elétrica

-- =============================================
-- DEVICES - João (10)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(100, 10, 10, 'NOVO',         '2024-06-01', 'Micro-ondas Samsung',       50.00, false, false, '{1}'),
(101, 10, 14, 'EM_ANDAMENTO', '2024-06-15', 'Micro-ondas embutir',       60.00, true,  false, '{1}'),
(102, 10, 12, 'ENTREGUE',     '2024-05-01', 'Micro-ondas Britânia',      70.00, false, true,  '{2}'),
(106, 10, 13, 'PRONTO',       '2024-08-01', 'Micro-ondas Britania',      65.00, true,  false, '{2}'),
(107, 10, 10, 'NOVO',         '2024-09-01', 'Outro Samsung',             45.00, false, false, '{1}'),
(108, 10, 20, 'NOVO',         '2024-10-01', 'Ventilador Samsung João',   25.00, false, false, '{1}'),
(109, 10, 21, 'NOVO',         '2024-10-15', 'Micro-ondas Britânia João', 55.00, false, false, '{2}'),
-- João: novos devices com typos de marca
(114, 10, 22, 'NOVO',         '2024-11-01', 'Micro-ondas Brastemp',      52.00, false, false, '{1}'),
(115, 10, 23, 'EM_ANDAMENTO', '2024-11-05', 'Micro-ondas Electrolux',    58.00, true,  false, '{2}'),
(116, 10, 24, 'NOVO',         '2024-11-10', 'Ventilador Consul',         33.00, false, false, '{3}'),
-- João: devices com typos de marca (variações)
(117, 10, 25, 'NOVO',         '2024-11-15', 'Micro-ondas Brastenp',      53.00, false, false, '{1}'),
(118, 10, 26, 'PRONTO',       '2024-11-20', 'Micro-ondas Eletrolux',     59.00, false, true,  '{2}'),
(119, 10, 27, 'NOVO',         '2024-11-25', 'Ventilador Consusl',        34.00, false, false, '{3}'),
-- João: devices com marcas reais novas
(120, 10, 28, 'NOVO',         '2024-12-01', 'Liquidificador Panasonic',  42.00, false, false, '{1}'),
(121, 10, 31, 'EM_ANDAMENTO', '2024-12-05', 'Aspirador Samsung',         38.00, true,  false, '{2}'),
(122, 10, 32, 'NOVO',         '2024-12-10', 'Chaleira Britânia',         29.00, false, false, '{3}');

-- =============================================
-- DEVICES - Maria (11)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(103, 11, 15, 'NOVO',         '2024-07-01', 'Ventilador Britânia',       30.00, false, false, '{3}'),
(104, 11, 16, 'AGUARDANDO',   '2024-07-10', 'Ventilador mesa',           35.00, true,  true,  '{3}'),
(105, 11, 11, 'DESCARTADO',   '2023-12-01', 'Micro-ondas Sansung',       40.00, false, false, '{1}'),
(110, 11, 20, 'NOVO',         '2024-11-01', 'Ventilador Samsung Maria',  28.00, false, false, '{3}'),
(111, 11, 21, 'NOVO',         '2024-11-15', 'Micro-ondas Britânia Maria', 48.00, false, false, '{1}'),
-- Maria: novos devices com typos de marca
(123, 11, 22, 'NOVO',         '2024-11-20', 'Micro-ondas Brastemp',      54.00, false, false, '{1}'),
(124, 11, 26, 'EM_ANDAMENTO', '2024-11-25', 'Micro-ondas Eletrolux',     61.00, true,  false, '{2}'),
(125, 11, 28, 'NOVO',         '2024-12-01', 'Liquidificador Panasonic',  43.00, false, false, '{3}'),
-- Maria: devices com typos de marca (variações)
(126, 11, 25, 'NOVO',         '2024-12-05', 'Micro-ondas Brastenp',      55.00, false, false, '{1}'),
(127, 11, 29, 'PRONTO',       '2024-12-10', 'Liquidificador Panassonic', 44.00, false, true,  '{2}'),
-- Maria: devices com tipos novos
(128, 11, 34, 'NOVO',         '2024-12-15', 'Ventilador Wallita',        26.00, false, false, '{3}'),
(129, 11, 35, 'EM_ANDAMENTO', '2024-12-20', 'Secadora Electrolux',       67.00, true,  false, '{1}');

-- =============================================
-- DEVICES - Pedro (12)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(112, 12, 18, 'NOVO',         '2024-12-01', 'Liquidificador Arno Pedro',    32.00, false, false, '{2}'),
(113, 12, 19, 'NOVO',         '2024-12-15', 'Ventilador Wallita Pedro',     27.00, false, false, '{3}'),
-- Pedro: devices com tipos novos
(130, 12, 30, 'NOVO',         '2024-12-20', 'Ferro a vapor Mondial',        36.00, false, false, '{1}'),
(131, 12, 33, 'EM_ANDAMENTO', '2024-12-25', 'Lava louças Arno',             41.00, true,  false, '{2}'),
(132, 12, 36, 'NOVO',         '2025-01-01', 'Aspirador Brastemp',           37.00, false, false, '{3}');

-- =============================================
-- DEVICES - Adilson Ferreira da Silva (13)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(133, 13, 10, 'NOVO',         '2024-06-01', 'Micro-ondas Samsung Adilson',     51.00, false, false, '{1}'),
(134, 13, 15, 'EM_ANDAMENTO', '2024-07-01', 'Ventilador Brastenp Adilson',     31.00, true,  false, '{2}'),
(135, 13, 31, 'NOVO',         '2024-08-01', 'Aspirador Samsung Adilson',       39.00, false, false, '{3}');

-- =============================================
-- DEVICES - Alessandro de Paula (14)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(136, 14, 23, 'NOVO',         '2024-06-15', 'Micro-ondas Electrolux Alessandro', 56.00, false, false, '{1}'),
(137, 14, 35, 'EM_ANDAMENTO', '2024-07-15', 'Secadora Electrolux Alessandro',    68.00, true,  false, '{2}');

-- =============================================
-- DEVICES - Maria Aparecida de Oliveira (15)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(138, 15, 25, 'NOVO',         '2024-06-20', 'Micro-ondas Brastenp Maria Aparecida', 57.00, false, false, '{1}'),
(139, 15, 34, 'PRONTO',       '2024-07-20', 'Ventilador Wallita Maria Aparecida',    29.00, false, true,  '{3}');

-- =============================================
-- DEVICES - José Carlos da Silva (16)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(140, 16, 27, 'NOVO',         '2024-06-25', 'Ventilador Consusl José Carlos',     35.00, false, false, '{3}'),
(141, 16, 37, 'EM_ANDAMENTO', '2024-07-25', 'Ferro a vapor Consul José Carlos',   40.00, true,  false, '{1}');

-- =============================================
-- DEVICES - Ana Beatriz Souza (17)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(142, 17, 28, 'NOVO',         '2024-06-30', 'Liquidificador Panasonic Ana',   44.00, false, false, '{2}'),
(143, 17, 38, 'AGUARDANDO',   '2024-07-30', 'Chaleira Panasonic Ana',         30.00, true,  true,  '{3}');

-- =============================================
-- DEVICES - Paulo Henrique Santos (18)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(144, 18, 30, 'NOVO',         '2024-07-01', 'Ferro a vapor Mondial Paulo',    37.00, false, false, '{1}'),
(145, 18, 24, 'EM_ANDAMENTO', '2024-08-01', 'Ventilador Consul Paulo',        34.00, true,  false, '{2}');

-- =============================================
-- DEVICES - João Pedro da Silva (19)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(146, 19, 10, 'NOVO',         '2024-06-01', 'Micro-ondas Samsung João Pedro',       50.00, false, false, '{1}'),
(147, 19, 15, 'EM_ANDAMENTO', '2024-07-01', 'Ventilador Britânia João Pedro',       30.00, true,  false, '{3}'),
(148, 19, 28, 'PRONTO',       '2024-08-01', 'Liquidificador Panasonic João Pedro',  42.00, false, true,  '{2}');

-- =============================================
-- DEVICES - João Paulo Ferreira (20)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(149, 20, 22, 'NOVO',         '2024-06-15', 'Micro-ondas Brastemp João Paulo',      52.00, false, false, '{1}'),
(150, 20, 24, 'AGUARDANDO',   '2024-07-15', 'Ventilador Consul João Paulo',         33.00, true,  true,  '{3}'),
(151, 20, 31, 'EM_ANDAMENTO', '2024-08-15', 'Aspirador Samsung João Paulo',         38.00, false, false, '{2}');

-- =============================================
-- DEVICES - João Victor Almeida Santos (21)
-- =============================================
INSERT INTO devices(id, id_customer, id_brand_model_type, device_status,
    entry_date, problem, labor_value, has_urgency, is_revision, color_ids) VALUES
(152, 21, 12, 'NOVO',         '2024-06-20', 'Micro-ondas Britânia João Victor',     55.00, false, false, '{1}'),
(153, 21, 16, 'PRONTO',       '2024-07-20', 'Ventilador Britânia João Victor',      35.00, false, true,  '{3}'),
(154, 21, 35, 'EM_ANDAMENTO', '2024-08-20', 'Secadora Electrolux João Victor',      67.00, true,  false, '{2}');

-- =============================================
-- PHONES (para testes de customerPhone)
-- =============================================
INSERT INTO phones(id, number, alias) VALUES
(100, '11999887766', 'Celular João');

INSERT INTO customer_phones(id_customer, id_phone, is_main) VALUES
(10, 100, true);
