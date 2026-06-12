SELECT
    c.id,
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
ORDER BY
    CASE
        WHEN :SEARCH_NAME IS NOT NULL AND :SEARCH_NAME != ''
        AND LOWER(unaccent(c.name)) LIKE LOWER(unaccent(:SEARCH_NAME)) || '%'
        THEN 0
        ELSE 1
    END,
    CASE
        WHEN :SEARCH_NAME IS NOT NULL AND :SEARCH_NAME != ''
        THEN word_similarity(
            LOWER(unaccent(:SEARCH_NAME)),
            LOWER(unaccent(c.name))
        )
    END DESC NULLS LAST,
    CASE
        WHEN :SEARCH_NAME IS NOT NULL AND :SEARCH_NAME != ''
        THEN LENGTH(c.name)
    END ASC NULLS LAST,
    CASE
        WHEN :SEARCH_EMAIL IS NOT NULL AND :SEARCH_EMAIL != ''
        AND LOWER(unaccent(c.email)) LIKE LOWER(unaccent(:SEARCH_EMAIL)) || '%'
        THEN 0
        ELSE 1
    END,
    CASE
        WHEN :SEARCH_EMAIL IS NOT NULL AND :SEARCH_EMAIL != ''
        THEN word_similarity(
            LOWER(unaccent(:SEARCH_EMAIL)),
            LOWER(unaccent(c.email))
        )
    END DESC NULLS LAST,
    CASE
        WHEN :SEARCH_EMAIL IS NOT NULL AND :SEARCH_EMAIL != ''
        THEN LENGTH(c.email)
    END ASC NULLS LAST,
    CASE WHEN :ORDER_BY_DIRECTION = 'ASC' THEN
        CASE :ORDER_BY_FIELD
            WHEN 'name'  THEN c.name
            WHEN 'email' THEN c.email
            WHEN 'cpf'   THEN c.cpf
        END
    END ASC,
    CASE WHEN :ORDER_BY_DIRECTION = 'DESC' THEN
        CASE :ORDER_BY_FIELD
            WHEN 'name'  THEN c.name
            WHEN 'email' THEN c.email
            WHEN 'cpf'   THEN c.cpf
        END
    END DESC,
    c.name ASC,
    c.id ASC
LIMIT :PAGE_SIZE OFFSET :OFFSET
