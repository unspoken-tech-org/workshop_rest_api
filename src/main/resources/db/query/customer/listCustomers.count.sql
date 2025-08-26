SELECT COUNT(*)
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
;