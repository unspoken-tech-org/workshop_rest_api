select d.id as device_id, c.id as customer_id, c."name" as customer_name, t."type", b.brand, m.model,
d.device_status as "status", d.problem, d.observation, d.has_urgency, d.is_revision as has_revision, d.entry_date, d.departure_date
from devices d
left join customers c on d.id_customer = c.id
left join brands_models_types bmt on bmt.id = d.id_brand_model_type
left join brands b on b.id = bmt.id_brand
left join models m on m.id = bmt.id_model
left join "types" t on t.id  = bmt.id_type
where
    (:DEVICE_ID IS NULL OR d.id = :DEVICE_ID)
    AND (:CUSTOMER_NAME IS NULL OR (:CUSTOMER_NAME != '' AND LOWER(unaccent(c.name)) ilike CONCAT('%', LOWER(unaccent(:CUSTOMER_NAME)), '%')))
        AND (:CUSTOMER_PHONE IS NULL 
        OR (:CUSTOMER_PHONE != '' 
            AND EXISTS(
            SELECT 1 FROM customer_phones cp
            LEFT JOIN phones p on p.id = cp.id_phone
            WHERE cp.id_customer = d.id_customer
            AND p.number ILIKE CONCAT('%', :CUSTOMER_PHONE, '%')
            )
        )
    )
    AND (:CUSTOMER_CPF IS NULL OR (:CUSTOMER_CPF != '' AND c.cpf ilike CONCAT('%', :CUSTOMER_CPF, '%')))
    AND (COALESCE(array_length(:STATUS::text[], 1), 0) = 0 OR d.device_status = ANY(:STATUS::text[]))
    AND (COALESCE(array_length(:DEVICE_TYPES::int[], 1), 0) = 0 OR t.id = ANY(:DEVICE_TYPES::int[]))
    AND (COALESCE(array_length(:DEVICE_BRANDS::int[], 1), 0) = 0 OR b.id = ANY(:DEVICE_BRANDS::int[]))
    AND ((:INITIAL_ENTRY_DATE IS NULL AND :FINAL_ENTRY_DATE IS NULL) 
        OR ((:INITIAL_ENTRY_DATE IS NOT NULL AND :FINAL_ENTRY_DATE IS NOT NULL)
            AND d.entry_date BETWEEN :INITIAL_ENTRY_DATE::timestamp AND :FINAL_ENTRY_DATE::timestamp
        )
        OR ((:INITIAL_ENTRY_DATE IS NOT NULL AND :FINAL_ENTRY_DATE IS NULL)
            AND d.entry_date >= :INITIAL_ENTRY_DATE::timestamp
        )
        OR ((:INITIAL_ENTRY_DATE IS NULL AND :FINAL_ENTRY_DATE IS NOT NULL)
            AND d.entry_date <= :FINAL_ENTRY_DATE::timestamp
        )
    )
        
    AND (:HAS_URGENCY IS NULL OR d.has_urgency = :HAS_URGENCY)
    AND (:HAS_REVISION IS NULL OR d.is_revision = :HAS_REVISION)
ORDER BY
    -- ASC (default)
    CASE 
        WHEN :ORDER_BY_DIRECTION = 'ASC' THEN
            CASE :ORDER_BY_FIELD
                WHEN 'name'  THEN c.name
                WHEN 'status'  THEN d.device_status 
                WHEN 'entryDate'     THEN d.entry_date::text  -- Cast timestamp to text
                -- add other columns here, always with cast if not text
            END
    END ASC,
    -- DESC
    CASE 
        WHEN :ORDER_BY_DIRECTION = 'DESC' THEN
            CASE :ORDER_BY_FIELD
                WHEN 'name'  THEN c.name
                WHEN 'status'  THEN d.device_status 
                WHEN 'entryDate'     THEN d.entry_date::text  -- Cast timestamp to text
                -- add other columns here, always with cast if not text
            END
    END DESC,
    -- Default (Fallback) to ensure consistency
    d.entry_date DESC
LIMIT :PAGE_SIZE OFFSET :OFFSET
