SELECT
  c.id,
  c."name",
  c.cpf,
  c.email,
  c.gender,
  c.insert_date,
  customer_phones_data.customer_phones AS phones,
  customer_devices.customer_devices
FROM customers c
LEFT JOIN LATERAL (
  SELECT COALESCE(json_agg(jsonb_build_object(
      'id', p.id,
      'number', p.number,
      'main', cp.is_main,
      'name', p.alias
  )), '[]'::json) AS customer_phones
  FROM customer_phones cp
  INNER JOIN phones p ON p.id = cp.id_phone
  WHERE cp.id_customer = c.id
) customer_phones_data ON true
LEFT JOIN LATERAL (
 SELECT COALESCE(json_agg(jsonb_build_object(
      'deviceId', d.id,
      'customerId', d.id_customer,
      'typeBrandModel', (select concat_ws(' ', t."type", b.brand, '|', m.model)),
      'deviceStatus', d.device_status,
      'problem', d.problem,
      'hasUrgency', d.has_urgency,
      'revision', d.is_revision,
      'entryDate', d.entry_date,
       'departureDate', d.departure_date
  ) order by d.entry_date desc), '[]'::json) AS customer_devices
  FROM devices d
  LEFT JOIN brands_models_types bmt on bmt.id = d.id_brand_model_type
  LEFT JOIN brands b on b.id = bmt.id_brand
  LEFT JOIN models m on m.id = bmt.id_model
  LEFT JOIN "types" t on t.id = bmt.id_type
  WHERE d.id_customer = c.id
) customer_devices ON TRUE
WHERE (:CUSTOMER_ID IS NULL OR c.id = :CUSTOMER_ID)