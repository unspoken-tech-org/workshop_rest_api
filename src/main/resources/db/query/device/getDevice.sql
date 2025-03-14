SELECT d.id AS device_id, c.id AS customer_id, c."name" AS customer_name,
ds.status AS device_status, b.brand AS brand_name, m.model AS model_name, t."type" AS type_name,
tec.id AS technician_id, tec.technician AS technician_name, d.problem, d.observation, d.has_urgency
from devices d
JOIN customers c ON d.id_customer = c.id
JOIN device_status ds ON ds.id = d.id_device_status
JOIN brands_models_types bmt ON bmt.id = d.id_brand_model_type
JOIN brands b ON b.id = bmt.id_brand
JOIN models m ON m.id = bmt.id_model
JOIN "types" t ON t.id = bmt.id_type
JOIN technicians tec ON tec.id = d.id_technician
WHERE d.id = :DEVICE_ID
