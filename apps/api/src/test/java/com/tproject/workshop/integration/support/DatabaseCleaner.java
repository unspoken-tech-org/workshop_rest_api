package com.tproject.workshop.integration.support;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Dynamically truncates all tables in the current PostgreSQL schema except migration metadata
 * tables ({@code flyway_schema_history} and {@code migration_marker}).
 */
public final class DatabaseCleaner {

    private static final Set<String> EXCLUDED_TABLES =
            Set.of("flyway_schema_history", "migration_marker");

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    public void clean() {
        List<String> tables =
                jdbcTemplate.queryForList(
                        """
                        SELECT tablename
                        FROM pg_tables
                        WHERE schemaname = current_schema()
                        ORDER BY tablename
                        """,
                        String.class);

        String targetTables =
                tables.stream()
                        .filter(table -> !EXCLUDED_TABLES.contains(table.toLowerCase(Locale.ROOT)))
                        .map(DatabaseCleaner::quoteIdentifier)
                        .collect(Collectors.joining(", "));

        if (!targetTables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE " + targetTables + " RESTART IDENTITY CASCADE");
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
