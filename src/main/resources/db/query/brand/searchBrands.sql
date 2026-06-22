SELECT
    b.id AS id_brand,
    b.brand
FROM brands b
WHERE
    1=1
    AND
    (:QUERY IS NULL OR :QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(unaccent(word))
                <% LOWER(unaccent(b.brand))
            )
            FROM unnest(string_to_array(:QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
ORDER BY
    CASE
        WHEN :QUERY IS NOT NULL AND :QUERY != ''
        AND LOWER(unaccent(b.brand)) LIKE LOWER(unaccent(:QUERY)) || '%'
        THEN 0
        ELSE 1
    END,
    CASE
        WHEN :QUERY IS NOT NULL AND :QUERY != ''
        THEN word_similarity(
            LOWER(unaccent(:QUERY)),
            LOWER(unaccent(b.brand))
        )
    END DESC NULLS LAST,
    LENGTH(b.brand) ASC,
    b.brand ASC,
    b.id ASC
LIMIT :PAGE_SIZE OFFSET :OFFSET
