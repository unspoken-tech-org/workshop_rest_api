select d.id as device_id, c.id as customer_id, c."name" as customer_name, t."type", b.brand, m.model, ds.status, d.problem, d.observation, d.has_urgency, d.entry_date, d.departure_date
from devices d
left join customers c on d.id_customer = c.id
left join brands_models_types bmt on bmt.id = d.id_brand_model_type
left join brands b on b.id = bmt.id_brand
left join models m on m.id = bmt.id_model
left join "types" t on t.id  = bmt.id_type
left join device_status ds on ds.id = d.id_device_status
where
    (:DEVICE_ID IS NULL OR d.id = :DEVICE_ID)
    AND (:CUSTOMER_NAME IS NULL OR c.name ilike CONCAT('%', :CUSTOMER_NAME, '%'))
    AND (:CUSTOMER_PHONE IS NULL OR c.phone ilike CONCAT('%', :CUSTOMER_PHONE, '%') OR c.whatsapp ilike CONCAT('%', :CUSTOMER_PHONE, '%'))
    AND (:CUSTOMER_CPF IS NULL OR c.cpf ilike CONCAT('%', :CUSTOMER_CPF, '%'))
    AND (COALESCE(array_length(:STATUS::text[], 1), 0) = 0 OR ds.status = ANY(:STATUS::text[]))
