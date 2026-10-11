SELECT COUNT(DISTINCT m.id)
FROM models m
INNER JOIN brands_models_types bmt ON bmt.id_model = m.id
WHERE
    bmt.id_brand = :BRAND_ID
    AND bmt.id_type = :TYPE_ID
    AND (:QUERY IS NULL OR :QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(unaccent(word))
                <% LOWER(unaccent(m.model))
            )
            FROM unnest(string_to_array(:QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
