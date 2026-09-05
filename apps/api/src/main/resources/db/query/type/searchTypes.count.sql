SELECT COUNT(*)
FROM types t
WHERE
    (:QUERY IS NULL OR :QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(unaccent(word))
                <% LOWER(unaccent(t.type))
            )
            FROM unnest(string_to_array(:QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
