INSERT INTO customers (id, name, cpf, gender, insert_date, email) VALUES
(1, 'Test Customer CContact', '11122233344', 'masculino', NOW(), 'customer.ccontact@example.com')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    cpf = EXCLUDED.cpf,
    gender = EXCLUDED.gender,
    insert_date = EXCLUDED.insert_date,
    email = EXCLUDED.email;

INSERT INTO device_status(id, status)
VALUES
(1, 'novo'),
(2, 'em_andamento'),
(3, 'aguardando'),
(4, 'entregue'),
(5, 'descartado'),
(6, 'pronto')
ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;

INSERT INTO technicians (id, technician_name, "number") VALUES
(1, 'Default Tech', 'T001'),
(2, 'Contact Test Tech', 'T002')
ON CONFLICT (id) DO UPDATE SET
    technician_name = EXCLUDED.technician_name,
    "number" = EXCLUDED."number";

INSERT INTO phones (id, "number", is_main, id_customer) VALUES
(2, '9876543210', true, 1)
ON CONFLICT (id) DO UPDATE SET
    "number" = EXCLUDED."number",
    is_main = EXCLUDED.is_main,
    id_customer = EXCLUDED.id_customer;

INSERT INTO brands (id, brand) VALUES (1, 'GenericBrand')
ON CONFLICT (id) DO UPDATE SET brand = EXCLUDED.brand;
INSERT INTO models (id, model) VALUES (1, 'GenericModel')
ON CONFLICT (id) DO UPDATE SET model = EXCLUDED.model;
INSERT INTO types (id, type) VALUES (1, 'GenericType')
ON CONFLICT (id) DO UPDATE SET type = EXCLUDED.type;

INSERT INTO brands_models_types (id, id_brand, id_model, id_type) VALUES
(1, 1, 1, 1)
ON CONFLICT (id) DO UPDATE SET
    id_brand = EXCLUDED.id_brand,
    id_model = EXCLUDED.id_model,
    id_type = EXCLUDED.id_type;

INSERT INTO colors (id, color) VALUES (1, 'Black'), (2, 'White')
ON CONFLICT (id) DO UPDATE SET color = EXCLUDED.color;


INSERT INTO devices (id, entry_date, problem, observation, budget, labor_value, service_value, labor_value_collected, has_urgency, is_revision, color_ids, last_update, id_customer, id_device_status, id_technician, id_brand_model_type) VALUES
(1, NOW(), 'Initial problem statement', 'No specific observations.', '150.00', 100.00, 50.00, false, false, false, ARRAY[1], NOW(), 1, 2, 1, 1)
ON CONFLICT (id) DO UPDATE SET
    entry_date = EXCLUDED.entry_date,
    problem = EXCLUDED.problem,
    observation = EXCLUDED.observation,
    budget = EXCLUDED.budget,
    labor_value = EXCLUDED.labor_value,
    service_value = EXCLUDED.service_value,
    labor_value_collected = EXCLUDED.labor_value_collected,
    has_urgency = EXCLUDED.has_urgency,
    is_revision = EXCLUDED.is_revision,
    color_ids = EXCLUDED.color_ids,
    last_update = EXCLUDED.last_update,
    id_customer = EXCLUDED.id_customer,
    id_device_status = EXCLUDED.id_device_status,
    id_technician = EXCLUDED.id_technician,
    id_brand_model_type = EXCLUDED.id_brand_model_type;


INSERT INTO customer_contact (id_device, id_technician, phone, type, has_made_contact, last_contact, conversation, id_device_status) VALUES
(1, 1, '9876543210', 'ligacao', false, '2024-01-10 09:00:00.000000', 'Previous contact attempt, no answer.', 2)
ON CONFLICT (id) DO NOTHING;

