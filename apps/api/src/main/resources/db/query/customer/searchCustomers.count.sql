SELECT COUNT(*)
FROM
    customers c
WHERE
    (:ID IS NULL OR c.id = :ID)
    AND (:CPF IS NULL OR (:CPF != '' AND c.cpf = :CPF))
    AND (
        :PHONE IS NULL
        OR (:PHONE != '' AND EXISTS (
            SELECT 1
            FROM customer_phones cp
            INNER JOIN phones p ON p.id = cp.id_phone
            WHERE cp.id_customer = c.id AND p.number ILIKE '%' || :PHONE || '%'
        ))
    )
    AND (:SEARCH_NAME IS NULL OR :SEARCH_NAME = ''
        OR (
            SELECT bool_and(
                LOWER(unaccent(word))
                <% LOWER(unaccent(c.name))
            )
            FROM unnest(string_to_array(:SEARCH_NAME, ' ')) AS word
            WHERE word <> ''
        )
    )
    AND (:SEARCH_EMAIL IS NULL OR :SEARCH_EMAIL = ''
        OR (
            SELECT bool_and(
                LOWER(unaccent(word))
                <% LOWER(unaccent(c.email))
            )
            FROM unnest(string_to_array(:SEARCH_EMAIL, ' ')) AS word
            WHERE word <> ''
        )
    )
