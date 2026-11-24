SELECT
    c.id as id,
    c.name,
    c.cpf,
    c.email,
    c.gender,
    c.created_at,
    (SELECT p.number 
     FROM customer_phones cp 
     INNER JOIN phones p ON p.id = cp.id_phone 
     WHERE cp.id_customer = c.id AND cp.is_main = TRUE 
     LIMIT 1) as main_phone
FROM
    customers c
WHERE
    (:CUSTOMER_ID IS NULL OR c.id = :CUSTOMER_ID) 
    AND (:NAME IS NULL OR (:NAME != '' AND LOWER(immutable_unaccent(c.name)) ILIKE '%' || REPLACE(REPLACE(REPLACE(LOWER(immutable_unaccent(:NAME)), '\', '\\'), '%', '\%'), '_', '\_') || '%' ESCAPE '\')) 
    AND (:CPF IS NULL OR (:CPF != '' AND c.cpf = :CPF))
    AND (
      :PHONE IS NULL
      OR (:PHONE != '' AND EXISTS (
        SELECT 1
        FROM customer_phones cp
        INNER JOIN phones p ON p.id = cp.id_phone
        WHERE cp.id_customer = c.id AND p.number ILIKE '%' || REPLACE(REPLACE(REPLACE(:PHONE, '\', '\\'), '%', '\%'), '_', '\_') || '%' ESCAPE '\'
        )
      )
    )
ORDER BY
    c.name ASC,
    c.id ASC
LIMIT :PAGE_SIZE OFFSET :OFFSET;