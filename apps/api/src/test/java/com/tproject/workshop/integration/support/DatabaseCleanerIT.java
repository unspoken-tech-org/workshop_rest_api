package com.tproject.workshop.integration.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseCleanerIT extends AbstractIntegrationTest {

    @Test
    void shouldCleanNewlyCreatedTableWithoutUpdatingAllowlistAndPreserveMigrationMetadata() {
        DatabaseCleaner cleaner = new DatabaseCleaner(jdbcTemplate);

        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS dynamic_cleaner_probe (id SERIAL PRIMARY KEY, label TEXT NOT NULL)");
        try {
            jdbcTemplate.update(
                    "INSERT INTO dynamic_cleaner_probe (label) VALUES (?)", "probe-row");
            Integer rowsBeforeClean =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM dynamic_cleaner_probe", Integer.class);
            assertThat(rowsBeforeClean).isEqualTo(1);

            Integer migrationMarkerBefore =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM migration_marker", Integer.class);
            Integer flywayHistoryBefore =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);

            cleaner.clean();

            Integer rowsAfterClean =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM dynamic_cleaner_probe", Integer.class);
            Integer migrationMarkerAfter =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM migration_marker", Integer.class);
            Integer flywayHistoryAfter =
                    jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM flyway_schema_history", Integer.class);

            assertThat(rowsAfterClean).isZero();
            assertThat(migrationMarkerBefore).isPositive();
            assertThat(migrationMarkerAfter).isEqualTo(migrationMarkerBefore);
            assertThat(flywayHistoryBefore).isPositive();
            assertThat(flywayHistoryAfter).isEqualTo(flywayHistoryBefore);
        } finally {
            jdbcTemplate.execute("DROP TABLE IF EXISTS dynamic_cleaner_probe");
        }
    }
}
