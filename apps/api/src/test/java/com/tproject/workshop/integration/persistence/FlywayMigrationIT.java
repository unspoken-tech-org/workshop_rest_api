package com.tproject.workshop.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies Flyway migrations against real pre-existing data in an isolated schema (I9), ensuring
 * N-1 to N upgrades preserve existing rows and data-reconciliation migrations behave as expected.
 */
class FlywayMigrationIT extends AbstractIntegrationTest {

    private static final String TEST_SCHEMA = "flyway_migration_test";
    private static final String MIGRATIONS_LOCATION = "classpath:db/migration/postgresql";

    @Autowired private DataSource dataSource;

    @AfterEach
    void dropIsolatedSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TEST_SCHEMA + " CASCADE");
    }

    @Test
    void shouldMigrateFromPreviousVersionToLatestPreservingExistingData() {
        recreateIsolatedSchema();

        Flyway discovery = configureFlyway(null);
        List<MigrationInfo> migrations =
                Arrays.stream(discovery.info().all())
                        .sorted(Comparator.comparing(MigrationInfo::getVersion))
                        .toList();
        assertThat(migrations).hasSizeGreaterThanOrEqualTo(2);

        MigrationVersion previousVersion = migrations.get(migrations.size() - 2).getVersion();
        MigrationVersion latestVersion = migrations.get(migrations.size() - 1).getVersion();

        configureFlyway(previousVersion.getVersion()).migrate();

        seedBaselineRowsInIsolatedSchema();

        MigrateResult upgradeResult =
                configureFlyway(MigrationVersion.LATEST.getVersion()).migrate();

        assertThat(upgradeResult.migrationsExecuted).isEqualTo(1);
        assertThat(upgradeResult.targetSchemaVersion).isEqualTo(latestVersion.getVersion());

        Integer customerCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM "
                                + TEST_SCHEMA
                                + ".customers WHERE cpf = '31781477051'",
                        Integer.class);
        Integer deviceCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM "
                                + TEST_SCHEMA
                                + ".devices WHERE problem = 'Tela trincada'",
                        Integer.class);
        Integer paymentCount =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM "
                                + TEST_SCHEMA
                                + ".payments WHERE payment_value = 150.00",
                        Integer.class);
        String apiKeyName =
                jdbcTemplate.queryForObject(
                        "SELECT client_name FROM " + TEST_SCHEMA + ".api_keys WHERE id = 1",
                        String.class);

        assertThat(customerCount).isEqualTo(1);
        assertThat(deviceCount).isEqualTo(1);
        assertThat(paymentCount).isEqualTo(1);
        assertThat(apiKeyName).isEqualTo("legacy_client");
    }

    @Test
    void shouldReconcileBudgetFeeCollectedWhenMigratingThroughV16WithData() {
        recreateIsolatedSchema();

        configureFlyway("15").migrate();
        seedBaselineRowsInIsolatedSchema();

        // Second device has budget_fee = 50.00 but no taxa_orcamento payment.
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".devices (id, id_customer, id_brand_model_type, device_status, entry_date, problem, budget_fee, budget_fee_collected, has_urgency, is_revision, color_ids) "
                        + "VALUES (2, 1, 1, 'NOVO', CURRENT_TIMESTAMP, 'Sem pagamento taxa', 50.00, false, false, false, '{1}')");

        // First device has budget_fee = 50.00 and a matching taxa_orcamento payment of 50.00.
        jdbcTemplate.update(
                "UPDATE "
                        + TEST_SCHEMA
                        + ".devices SET budget_fee = 50.00, budget_fee_collected = false WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".payments (id, id_device, payment_date, payment_type, payment_value, category) "
                        + "VALUES (2, 1, CURRENT_TIMESTAMP, 'pix', 50.00, 'taxa_orcamento')");

        configureFlyway(MigrationVersion.LATEST.getVersion()).migrate();

        Boolean paidDeviceFlag =
                jdbcTemplate.queryForObject(
                        "SELECT budget_fee_collected FROM " + TEST_SCHEMA + ".devices WHERE id = 1",
                        Boolean.class);
        Boolean unpaidDeviceFlag =
                jdbcTemplate.queryForObject(
                        "SELECT budget_fee_collected FROM " + TEST_SCHEMA + ".devices WHERE id = 2",
                        Boolean.class);
        BigDecimal preservedServicePayment =
                jdbcTemplate.queryForObject(
                        "SELECT payment_value FROM " + TEST_SCHEMA + ".payments WHERE id = 1",
                        BigDecimal.class);

        assertThat(paidDeviceFlag).isTrue();
        assertThat(unpaidDeviceFlag).isFalse();
        assertThat(preservedServicePayment).isEqualByComparingTo("150.00");
    }

    @Test
    void shouldExpandDevicesIntoCustomerDevicesWhenMigratingThroughV20WithLegacyData() {
        recreateIsolatedSchema();
        configureFlyway("19").migrate();
        seedBaselineRowsInIsolatedSchema();

        // 2: ENTREGUE with departure date; 3: DESCARTADO without departure, closed in history;
        // 4: ENTREGUE without departure nor history, last_update before 16/06/2026;
        // 5: open with departure date (legacy, decision 6); 6: PRONTO whose revision was reset.
        insertLegacyDevice(
                2, "ENTREGUE", "'2026-07-01 10:00'", "'2026-07-10 15:00'", "'2026-07-10 15:00'");
        insertLegacyDevice(3, "DESCARTADO", "'2026-07-01 10:00'", "NULL", "'2026-08-01 09:00'");
        insertLegacyDevice(4, "ENTREGUE", "'2025-01-01 10:00'", "NULL", "'2025-02-01 09:00'");
        insertLegacyDevice(5, "PRONTO", "'2026-07-01 10:00'", "'2026-07-05 10:00'", "NULL");
        insertLegacyDevice(6, "PRONTO", "'2026-07-01 10:00'", "NULL", "NULL");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".device_history (id_device, field_name, old_value, new_value, history_date) VALUES "
                        + "(3, 'device_status', 'NAO_APROVADO', 'DESCARTADO', '2026-07-20 11:00'),"
                        + "(6, 'revision', 'false', 'true', '2026-07-02 10:00'),"
                        + "(6, 'revision', 'true', 'false', '2026-07-03 10:00')");

        configureFlyway(MigrationVersion.LATEST.getVersion()).migrate();

        Integer orphanOrders =
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM "
                                + TEST_SCHEMA
                                + ".devices d LEFT JOIN "
                                + TEST_SCHEMA
                                + ".customer_devices cd ON cd.id = d.id_customer_device"
                                + " WHERE cd.id IS NULL OR cd.id <> d.id"
                                + " OR cd.id_customer <> d.id_customer"
                                + " OR cd.id_brand_model_type <> d.id_brand_model_type"
                                + " OR cd.color_ids <> d.color_ids",
                        Integer.class);
        assertThat(orphanOrders).isZero();

        assertThat(closedAt(2)).isEqualTo("2026-07-10 15:00:00");
        assertThat(closedAt(3)).isEqualTo("2026-07-20 11:00:00");
        assertThat(closedAt(4)).isNull();
        assertThat(closedAt(5)).isNull();
        assertThat(revision(6)).isTrue();
        assertThat(revision(1)).isFalse();
        assertThat(warrantyDays(2)).isEqualTo(90);
        assertThat(warrantyDays(3)).isNull();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT departure_date IS NOT NULL FROM "
                                        + TEST_SCHEMA
                                        + ".devices WHERE id = 5",
                                Boolean.class))
                .isTrue();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT count(DISTINCT public_token) FROM "
                                        + TEST_SCHEMA
                                        + ".devices",
                                Integer.class))
                .isEqualTo(6);

        // The previous API image inserts orders without id_customer_device: the trigger creates the
        // physical device after the backfilled ids (decision 10).
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".devices (id, id_customer, id_brand_model_type, device_status, entry_date, problem, has_urgency, is_revision, color_ids) "
                        + "VALUES (7, 1, 1, 'NOVO', CURRENT_TIMESTAMP, 'Inserido pela imagem anterior', false, false, '{1}')");
        Integer newCustomerDeviceId =
                jdbcTemplate.queryForObject(
                        "SELECT cd.id FROM "
                                + TEST_SCHEMA
                                + ".devices d JOIN "
                                + TEST_SCHEMA
                                + ".customer_devices cd ON cd.id = d.id_customer_device"
                                + " WHERE d.problem = 'Inserido pela imagem anterior'"
                                + " AND cd.id_customer = 1 AND cd.id_brand_model_type = 1",
                        Integer.class);
        assertThat(newCustomerDeviceId).isEqualTo(7);
    }

    private void insertLegacyDevice(
            int id, String status, String entryDate, String departureDate, String lastUpdate) {
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".devices (id, id_customer, id_brand_model_type, device_status, entry_date, departure_date, last_update, problem, has_urgency, is_revision, color_ids) "
                        + "VALUES ("
                        + id
                        + ", 1, 1, '"
                        + status
                        + "', "
                        + entryDate
                        + ", "
                        + departureDate
                        + ", "
                        + lastUpdate
                        + ", 'Legado', false, false, '{1}')");
    }

    private String closedAt(int deviceId) {
        return jdbcTemplate.queryForObject(
                "SELECT to_char(closed_at, 'YYYY-MM-DD HH24:MI:SS') FROM "
                        + TEST_SCHEMA
                        + ".devices WHERE id = ?",
                String.class,
                deviceId);
    }

    private Boolean revision(int deviceId) {
        return jdbcTemplate.queryForObject(
                "SELECT is_revision FROM " + TEST_SCHEMA + ".devices WHERE id = ?",
                Boolean.class,
                deviceId);
    }

    private Integer warrantyDays(int deviceId) {
        return jdbcTemplate.queryForObject(
                "SELECT warranty_days FROM " + TEST_SCHEMA + ".devices WHERE id = ?",
                Integer.class,
                deviceId);
    }

    private void recreateIsolatedSchema() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TEST_SCHEMA + " CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA " + TEST_SCHEMA);
    }

    private Flyway configureFlyway(String target) {
        var config =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations(MIGRATIONS_LOCATION)
                        .schemas(TEST_SCHEMA)
                        .defaultSchema(TEST_SCHEMA)
                        .initSql("SET search_path TO " + TEST_SCHEMA + ", public");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private void seedBaselineRowsInIsolatedSchema() {
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".customers (id, name, created_at, cpf, gender, email) "
                        + "VALUES (1, 'Cliente Migracao', CURRENT_TIMESTAMP, '31781477051', 'M', 'migracao@example.com')");
        jdbcTemplate.update(
                "INSERT INTO " + TEST_SCHEMA + ".phones (id, number) VALUES (1, '44999990001')");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".customer_phones (id_customer, id_phone, is_main, alias) "
                        + "VALUES (1, 1, true, 'Principal')");
        jdbcTemplate.update(
                "INSERT INTO " + TEST_SCHEMA + ".brands (id, brand) VALUES (1, 'Samsung')");
        jdbcTemplate.update(
                "INSERT INTO " + TEST_SCHEMA + ".models (id, model) VALUES (1, 'Galaxy S24')");
        jdbcTemplate.update(
                "INSERT INTO " + TEST_SCHEMA + ".types (id, type) VALUES (1, 'Celular')");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".brands_models_types (id, id_brand, id_model, id_type) VALUES (1, 1, 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO " + TEST_SCHEMA + ".colors (id, color) VALUES (1, 'preto')");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".devices (id, id_customer, id_brand_model_type, device_status, entry_date, problem, budget_fee, budget_fee_collected, has_urgency, is_revision, color_ids) "
                        + "VALUES (1, 1, 1, 'NOVO', CURRENT_TIMESTAMP, 'Tela trincada', 0.00, false, false, false, '{1}')");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".payments (id, id_device, payment_date, payment_type, payment_value, category) "
                        + "VALUES (1, 1, CURRENT_TIMESTAMP, 'pix', 150.00, 'servicos')");
        jdbcTemplate.update(
                "INSERT INTO "
                        + TEST_SCHEMA
                        + ".api_keys (id, key_value, client_name, user_identifier, platform, role, description, active, created_at) "
                        + "VALUES (1, 'hash_legacy_key', 'legacy_client', 'legacy_user', 'MOBILE', 'ADMIN', 'Legacy key', true, CURRENT_TIMESTAMP)");
    }
}
