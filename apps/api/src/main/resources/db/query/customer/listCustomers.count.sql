SELECT COUNT(*)
FROM
    customers c
WHERE
    (:CUSTOMER_ID IS NULL OR c.id = :CUSTOMER_ID) 
    AND (:NAME IS NULL OR (:NAME != '' AND unaccent(c.name) ILIKE '%' || unaccent(:NAME) || '%')) 
    AND (:CPF IS NULL OR (:CPF != '' AND c.cpf = :CPF))
    AND (
      :PHONE IS NULL
      OR (:PHONE != '' AND EXISTS (
        SELECT 1
        FROM customer_phones cp
        INNER JOIN phones p ON p.id = cp.id_phone
        WHERE cp.id_customer = c.id AND p.number ILIKE '%' || :PHONE || '%'
        )
      )
    )
;