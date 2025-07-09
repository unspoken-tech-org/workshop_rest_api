SELECT
    c.id as id,
    c.name,
    c.cpf,
    c.email,
    c.gender,
    c.insert_date,
    (SELECT p.number FROM phones p WHERE p.id_customer = c.id AND p.is_main = TRUE LIMIT 1) as main_phone
FROM
    customers c
WHERE
    (:CUSTOMER_ID IS NULL OR c.id = :CUSTOMER_ID) AND
    (:NAME IS NULL OR c.name ILIKE '%' || :NAME || '%') AND
    (:CPF IS NULL OR c.cpf = :CPF) AND
    (
      :PHONE IS NULL OR
      EXISTS (
        SELECT 1
        FROM phones p
        WHERE p.id_customer = c.id AND p.number ILIKE '%' || :PHONE || '%'
      )
    )
ORDER BY
    c.name; 