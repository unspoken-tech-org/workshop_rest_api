SELECT
    t.id AS id_type,
    t.type
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
ORDER BY
    CASE
        WHEN :QUERY IS NOT NULL AND :QUERY != ''
        AND LOWER(immutable_unaccent(t.type)) LIKE LOWER(immutable_unaccent(:QUERY)) || '%'
        THEN 0
        ELSE 1
    END,
    CASE
        WHEN :QUERY IS NOT NULL AND :QUERY != ''
        THEN word_similarity(
            LOWER(immutable_unaccent(:QUERY)),
            LOWER(immutable_unaccent(t.type))
        )
    END DESC NULLS LAST,
    LENGTH(t.type) ASC,
    t.type ASC,
    t.id ASC
LIMIT :PAGE_SIZE OFFSET :OFFSET
