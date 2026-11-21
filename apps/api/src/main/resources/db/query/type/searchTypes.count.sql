SELECT COUNT(*)
FROM types t
WHERE
    (:QUERY IS NULL OR :QUERY = ''
        OR (
            SELECT bool_and(
                LOWER(immutable_unaccent(word))
                <% LOWER(immutable_unaccent(t.type))
            )
            FROM unnest(string_to_array(:QUERY, ' ')) AS word
            WHERE word <> ''
        )
    )
