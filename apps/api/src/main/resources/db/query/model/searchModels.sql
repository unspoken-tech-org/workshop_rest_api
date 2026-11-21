SELECT
    m.id AS id_model,
    m.model
FROM models m
INNER JOIN brands_models_types bmt ON bmt.id_model = m.id
WHERE
    bmt.id_brand = :BRAND_ID
    AND bmt.id_type = :TYPE_ID
    AND (:QUERY IS NULL OR :QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(immutable_unaccent(word))
                <% LOWER(immutable_unaccent(m.model))
            )
            FROM unnest(string_to_array(:QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
ORDER BY
    CASE
        WHEN :QUERY IS NOT NULL AND :QUERY != ''
        AND LOWER(immutable_unaccent(m.model)) LIKE LOWER(immutable_unaccent(:QUERY)) || '%'
        THEN 0
        ELSE 1
    END,
    CASE
        WHEN :QUERY IS NOT NULL AND :QUERY != ''
        THEN word_similarity(
            LOWER(immutable_unaccent(:QUERY)),
            LOWER(immutable_unaccent(m.model))
        )
    END DESC NULLS LAST,
    LENGTH(m.model) ASC,
    m.model ASC,
    m.id ASC
LIMIT :PAGE_SIZE OFFSET :OFFSET
