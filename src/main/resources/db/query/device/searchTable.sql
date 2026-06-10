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
    AND ((:INITIAL_ENTRY_DATE IS NULL AND :FINAL_ENTRY_DATE IS NULL) 
        OR ((:INITIAL_ENTRY_DATE IS NOT NULL AND :FINAL_ENTRY_DATE IS NOT NULL)
            AND d.entry_date >= :INITIAL_ENTRY_DATE::timestamp AND d.entry_date < :FINAL_ENTRY_DATE::timestamp + INTERVAL '1 day'
        )
        OR ((:INITIAL_ENTRY_DATE IS NOT NULL AND :FINAL_ENTRY_DATE IS NULL)
            AND d.entry_date >= :INITIAL_ENTRY_DATE::timestamp
        )
        OR ((:INITIAL_ENTRY_DATE IS NULL AND :FINAL_ENTRY_DATE IS NOT NULL)
            AND d.entry_date < :FINAL_ENTRY_DATE::timestamp + INTERVAL '1 day'
        )
    )
    AND (:HAS_URGENCY IS NULL OR d.has_urgency = :HAS_URGENCY)
    AND (:HAS_REVISION IS NULL OR d.is_revision = :HAS_REVISION)
    AND (:SEARCH_QUERY IS NULL OR :SEARCH_QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(unaccent(word))
                <% LOWER(CONCAT_WS(' ', unaccent(t.type), unaccent(b.brand), unaccent(m.model), unaccent(c.name)))
            )
            FROM unnest(string_to_array(:SEARCH_QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
ORDER BY
    -- 1. Relevância (só quando query presente)
    CASE
        WHEN :SEARCH_QUERY IS NOT NULL AND :SEARCH_QUERY != ''
        THEN word_similarity(
                LOWER(unaccent(:SEARCH_QUERY)),
                LOWER(CONCAT_WS(' ', unaccent(t.type), unaccent(b.brand), unaccent(m.model), unaccent(c.name)))
             )
    END DESC NULLS LAST,
    -- 2. Ordenação do usuário (sempre aplicada, field + direction)
    CASE
        WHEN :ORDER_BY_DIRECTION = 'ASC' THEN
            CASE :ORDER_BY_FIELD
                WHEN 'name'      THEN c.name
                WHEN 'status'    THEN d.device_status
                WHEN 'entryDate' THEN d.entry_date::text
            END
    END ASC,
    CASE
        WHEN :ORDER_BY_DIRECTION = 'DESC' THEN
            CASE :ORDER_BY_FIELD
                WHEN 'name'      THEN c.name
                WHEN 'status'    THEN d.device_status
                WHEN 'entryDate' THEN d.entry_date::text
            END
    END DESC,
    -- 3. Fallback
    d.entry_date DESC,
    d.id ASC
LIMIT :PAGE_SIZE OFFSET :OFFSET
