select d.id as device_id, c.id as customer_id, c."name" as customer_name, t."type", b.brand, m.model,
d.device_status as "status", d.problem, d.observation, d.has_urgency, d.is_revision as has_revision, d.entry_date, d.departure_date
from devices d
left join customers c on d.id_customer = c.id
left join brands_models_types bmt on bmt.id = d.id_brand_model_type
left join brands b on b.id = bmt.id_brand
left join models m on m.id = bmt.id_model
left join "types" t on t.id  = bmt.id_type
left join (
            select id_customer, string_agg(number, ', ') as numbers
            from phones
            group by id_customer
) p on p.id_customer = c.id
where
    (:DEVICE_ID IS NULL OR d.id = :DEVICE_ID)
    AND (:CUSTOMER_NAME IS NULL OR c.name ilike CONCAT('%', :CUSTOMER_NAME, '%'))
    AND (:CUSTOMER_PHONE IS NULL OR p.numbers ilike CONCAT('%', :CUSTOMER_PHONE, '%'))
    AND (:CUSTOMER_CPF IS NULL OR c.cpf ilike CONCAT('%', :CUSTOMER_CPF, '%'))
    AND (COALESCE(array_length(:STATUS::text[], 1), 0) = 0 OR d.device_status = ANY(:STATUS::text[]))
    AND (COALESCE(array_length(:DEVICE_TYPES::int[], 1), 0) = 0 OR t.id = ANY(:DEVICE_TYPES::int[]))
    AND (COALESCE(array_length(:DEVICE_BRANDS::int[], 1), 0) = 0 OR t.id = ANY(:DEVICE_BRANDS::int[]))
    AND (:INITIAL_ENTRY_DATE IS NULL OR :FINAL_ENTRY_DATE IS NULL OR d.entry_date BETWEEN :INITIAL_ENTRY_DATE::timestamp  AND :FINAL_ENTRY_DATE::timestamp )
    AND (:HAS_URGENCY IS FALSE OR d.has_urgency = :HAS_URGENCY)
    AND (:HAS_REVISION IS FALSE OR d.is_revision = :HAS_REVISION)
ORDER BY
    -- Ordenação ASC (padrão)
    CASE 
        WHEN :ORDER_BY_DIRECTION = 'ASC' THEN
            CASE :ORDER_BY_FIELD
                WHEN 'name'  THEN c.name
                WHEN 'status'  THEN d.device_status 
                WHEN 'entryDate'     THEN d.entry_date::text  -- Cast de timestamp para text
                -- adicione outras colunas aqui, sempre com cast se não for texto
            END
    END ASC,
    -- Ordenação DESC
    CASE 
        WHEN :ORDER_BY_DIRECTION = 'DESC' THEN
            CASE :ORDER_BY_FIELD
                WHEN 'name'  THEN c.name
                WHEN 'status'  THEN d.device_status 
                WHEN 'entryDate'     THEN d.entry_date::text  -- Cast de timestamp para text
                -- adicione outras colunas aqui, sempre com cast se não for texto
            END
    END DESC,
    -- Ordenação Padrão (Fallback) para garantir consistência
    d.entry_date DESC
