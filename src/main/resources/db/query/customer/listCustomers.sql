SELECT
    c.id as id,
    c.name,
    c.cpf,
    c.email,
    c.gender,
    c.insert_date,
    (SELECT p.number 
     FROM customer_phones cp 
     INNER JOIN phones p ON p.id = cp.id_phone 
     WHERE cp.id_customer = c.id AND cp.is_main = TRUE 
     LIMIT 1) as main_phone
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
ORDER BY
    c.name
LIMIT :PAGE_SIZE OFFSET :OFFSET;