select d.id as device_id, c.id as customer_id, c."name" as customer_name, t."type", b.brand, m.model, ds.status, d.problem, d.observation, d.has_urgency, d.entry_date, d.departure_date
from devices d
left join customers c on d.id = c.id
left join brands_models_types bmt on bmt.id = d.id_brand_model_type
left join brands_models bm on bm.id = bmt.id_brand_model
left join brands b on b.id = bm.id_brand
left join models m on m.id = bm.id_model
left join "types" t on t.id  = bmt.id_type
left join device_status ds on ds.id = d.id_device_status
where
    (COALESCE(array_length(:DEVICE_IDS::integer[], 1), 0) = 0 OR d.id = any(:DEVICE_IDS))
    AND (:CUSTOMER_ID IS NULL OR c.id = :CUSTOMER_ID)
    AND (:BRAND_ID IS NULL OR b.id = :BRAND_ID)
    AND (:MODEL_ID IS NULL OR m.id = :MODEL_ID)
    AND (:TYPE_ID IS NULL OR t.id = :TYPE_ID)
    AND (:STATUS_ID IS NULL OR ds.id = :STATUS_ID)
    AND (:CUSTOMER_NAME IS NULL OR c.name ilike CONCAT('%', :CUSTOMER_NAME, '%'))
