SELECT
  d.id AS device_id,
  c.id AS customer_id,
  c."name" AS customer_name,
  ds.status AS device_status,
  b.brand AS brand_name,
  m.model AS model_name,
  t."type" AS type_name,
  tec.id AS technician_id,
  tec.technician_name,
  d.problem,
  d.observation,
  d.budget,
  d.labor_value,
  d.service_value,
  d.labor_value_collected,
  d.has_urgency,
  d.is_revision,
  d.entry_date,
  d.departure_date,
  d.last_update,
  color_data.device_colors,
  contact_data.customer_contacts,
  customer_phones_data.customer_phones,
  customer_devices.other_devices,
  device_payments.payments
FROM devices d
JOIN customers c ON d.id_customer = c.id
JOIN device_status ds ON ds.id = d.id_device_status
JOIN brands_models_types bmt ON bmt.id = d.id_brand_model_type
JOIN brands b ON b.id = bmt.id_brand
JOIN models m ON m.id = bmt.id_model
JOIN "types" t ON t.id = bmt.id_type
JOIN technicians tec ON tec.id = d.id_technician
LEFT JOIN LATERAL (
  SELECT ARRAY_AGG(clr.color ORDER BY clr.color) AS device_colors
  FROM colors clr
  WHERE clr.id = ANY(d.color_ids)
) color_data ON true
LEFT JOIN LATERAL (
  SELECT COALESCE(json_agg(jsonb_build_object(
      'id', cp.id,
      'number', cp.number,
      'main', cp.is_main
  )), '[]'::json) AS customer_phones
  FROM phones cp
  WHERE cp.id_customer = c.id
) customer_phones_data ON true
LEFT JOIN LATERAL (
 SELECT COALESCE(json_agg(jsonb_build_object(
      'deviceId', od.id,
      'customerId', od.id_customer,
      'typeBrandModel', (select concat_ws(' ', ot."type", ob.brand, '|', om.model)),
      'deviceStatus', ods.status,
      'problem', od.problem,
      'hasUrgency', od.has_urgency,
      'revision', od.is_revision,
      'entryDate', od.entry_date,
       'departureDate', od.departure_date
  ) order by od.entry_date desc), '[]'::json) AS other_devices
  FROM devices od
  LEFT JOIN brands_models_types obmt on obmt.id = od.id_brand_model_type
  LEFT JOIN brands ob on ob.id = obmt.id_brand
  LEFT JOIN models om on om.id = obmt.id_model
  LEFT JOIN "types" ot on ot.id = obmt.id_type
  LEFT JOIN device_status ods on ods.id = od.id_device_status
  WHERE od.id_customer = c.id
) customer_devices ON TRUE
LEFT JOIN LATERAL (
  SELECT COALESCE(json_agg(jsonb_build_object(
      'id', cc.id,
      'deviceId', cc.id_device,
      'technicianId', cc.id_technician,
      'technicianName', technicians.technician_name,
      'phoneId', cc.id_phone,
      'phoneNumber', pc.number,
      'deviceStatus', dsj.status,
      'type', cc.type,
      'hasMadeContact', cc.has_made_contact,
      'lastContact', cc.last_contact,
      'conversation', cc.conversation
  ) ORDER BY cc.last_contact DESC), '[]'::json) AS customer_contacts
  FROM customer_contact cc
  LEFT JOIN device_status dsj ON dsj.id = cc.id_device_status
  LEFT JOIN technicians ON technicians.id = cc.id_technician
  LEFT JOIN phones pc ON pc.id = cc.id_phone
  WHERE cc.id_device = d.id
) contact_data ON true
LEFT JOIN LATERAL(
	SELECT COALESCE(json_agg(json_build_object(
		'paymentId', p.id,
		'paymentDate', p.payment_date,
		'paymentType', p.payment_type,
		'paymentValue', p.payment_value,
		'category', p.category
	) ORDER BY p.payment_date DESC), '[]'::json) as payments
	FROM payments p
	WHERE p.id_device = d.id
) device_payments ON true
WHERE d.id = :DEVICE_ID
