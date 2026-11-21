SELECT COUNT(DISTINCT b.id)
FROM brands b
WHERE
    (:QUERY IS NULL OR :QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(immutable_unaccent(word))
                <% LOWER(immutable_unaccent(b.brand))
            )
            FROM unnest(string_to_array(:QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
