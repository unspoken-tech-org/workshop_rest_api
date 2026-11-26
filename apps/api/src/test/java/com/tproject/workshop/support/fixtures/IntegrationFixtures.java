package com.tproject.workshop.support.fixtures;

import com.tproject.workshop.service.BrandService;
import com.tproject.workshop.service.ModelService;
import com.tproject.workshop.service.TypeService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Creates the minimum data an integration test needs and returns the database-generated IDs.
 * Catalog entries (brand, model, type) go through the application services so the helper keeps
 * working when catalog columns change; the remaining tables are written with plain SQL.
 */
public final class IntegrationFixtures {

    public static final String ADMIN_API_KEY = "sk_mobile_TEST_KEY_MOBILE_12345678901234567890";
    public static final String SERVICE_API_KEY = "sk_mobile_TEST_KEY_SERVICE_12345678901234567890";

    private final JdbcTemplate jdbc;
    private final ApplicationContext context;

    public IntegrationFixtures(JdbcTemplate jdbc, ApplicationContext context) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.context = Objects.requireNonNull(context);
    }

    // ---------------------------------------------------------------- API keys

    /** Inserts an active ADMIN MOBILE key whose plaintext is {@link #ADMIN_API_KEY}. */
    public long adminApiKey() {
        return apiKey(ADMIN_API_KEY, "test_app", "admin_user", "MOBILE", "ADMIN", true, null);
    }

    /** Inserts an active SERVICE MOBILE key whose plaintext is {@link #SERVICE_API_KEY}. */
    public long serviceApiKey() {
        return apiKey(
                SERVICE_API_KEY,
                "test_service_app",
                "service_user",
                "MOBILE",
                "SERVICE",
                true,
                null);
    }

    /** Inserts an API key storing the SHA-256 hash of {@code plaintext}; returns its id. */
    public long apiKey(
            String plaintext,
            String clientName,
            String userIdentifier,
            String platform,
            String role,
            boolean active,
            Instant expiresAt) {
        return jdbc.queryForObject(
                """
                INSERT INTO api_keys (key_value, client_name, user_identifier, platform, role,
                                      description, active, created_at, expires_at)
                VALUES (encode(sha256(convert_to(?, 'UTF8')), 'hex'), ?, ?, ?, ?, ?, ?,
                        CURRENT_TIMESTAMP, ?)
                RETURNING id
                """,
                Long.class,
                plaintext,
                clientName,
                userIdentifier,
                platform,
                role,
                "fixture " + clientName,
                active,
                expiresAt == null ? null : java.sql.Timestamp.from(expiresAt));
    }

    /** Inserts a refresh token (stored hashed) for an API key, already expired. */
    public void expiredRefreshToken(String plaintext, long apiKeyId) {
        jdbc.update(
                """
                INSERT INTO refresh_tokens (token, api_key_id, expires_at, revoked, created_at)
                VALUES (encode(sha256(convert_to(?, 'UTF8')), 'hex'), ?,
                        CURRENT_TIMESTAMP - INTERVAL '1 hour', false,
                        CURRENT_TIMESTAMP - INTERVAL '1 day')
                """,
                plaintext,
                apiKeyId);
    }

    // ----------------------------------------------------------------- catalog

    public int brand(String name) {
        return context.getBean(BrandService.class).createBrand(name).idBrand();
    }

    public int model(String name) {
        return context.getBean(ModelService.class).createModel(name).idModel();
    }

    public int type(String name) {
        return context.getBean(TypeService.class).createType(name).idType();
    }

    /** Links an existing brand, model and type; returns the brands_models_types id. */
    public int brandModelType(int brandId, int modelId, int typeId) {
        return jdbc.queryForObject(
                """
                INSERT INTO brands_models_types (id_brand, id_model, id_type)
                VALUES (?, ?, ?) ON CONFLICT (id_brand, id_model, id_type)
                DO UPDATE SET updated_at = CURRENT_TIMESTAMP RETURNING id
                """,
                Integer.class,
                brandId,
                modelId,
                typeId);
    }

    /** Creates (or reuses) the brand, model and type by name and links them. */
    public int brandModelType(String brand, String model, String type) {
        return brandModelType(brand(brand), model(model), type(type));
    }

    /** A generic, already linked brand/model/type; returns the brands_models_types id. */
    public int anyBrandModelType() {
        return brandModelType("GenericBrand", "GenericModel", "GenericType");
    }

    // ---------------------------------------------------------------- business

    public int customer(String name, String cpf, String gender, String email) {
        return customer(name, cpf, gender, email, LocalDateTime.of(2021, 4, 26, 8, 41));
    }

    public int customer(
            String name, String cpf, String gender, String email, LocalDateTime createdAt) {
        return jdbc.queryForObject(
                "INSERT INTO customers (name, created_at, cpf, gender, email)"
                        + " VALUES (?, ?, ?, ?, ?) RETURNING id",
                Integer.class,
                name,
                java.sql.Timestamp.valueOf(createdAt),
                cpf,
                gender,
                email);
    }

    /** Creates a customer with a default CPF/e-mail derived from the name. */
    public int customer(String name) {
        return customer(name, "31781477051", "masculino", "customer@example.com");
    }

    /** Creates a phone and links it to the customer; returns the phone id. */
    public int customerPhone(int customerId, String number, boolean main, String alias) {
        int phoneId =
                jdbc.queryForObject(
                        "INSERT INTO phones (number) VALUES (?) RETURNING id",
                        Integer.class,
                        number);
        jdbc.update(
                "INSERT INTO customer_phones (id_customer, id_phone, is_main, alias)"
                        + " VALUES (?, ?, ?, ?)",
                customerId,
                phoneId,
                main,
                alias);
        return phoneId;
    }

    public int technician(String name, String number) {
        return jdbc.queryForObject(
                "INSERT INTO technicians (technician_name, \"number\") VALUES (?, ?) RETURNING id",
                Integer.class,
                name,
                number);
    }

    public int color(String name) {
        return jdbc.queryForObject(
                "INSERT INTO colors (color) VALUES (?) RETURNING id", Integer.class, name);
    }

    public int device(int customerId, int brandModelTypeId, int technicianId, String status) {
        return jdbc.queryForObject(
                """
                INSERT INTO devices (id_customer, id_brand_model_type, device_status,
                    id_technician, entry_date, problem, budget_fee, service_value,
                    has_urgency, is_revision, color_ids)
                VALUES (?, ?, ?, ?, '2023-04-10 08:41:00', 'Problem', 50.00, 500.00,
                    false, false, '{}'::int[])
                RETURNING id
                """,
                Integer.class,
                customerId,
                brandModelTypeId,
                status,
                technicianId);
    }

    public int payment(int deviceId, String paymentType, String value, String receivedBy) {
        return jdbc.queryForObject(
                "INSERT INTO payments (id_device, payment_date, payment_type, payment_value,"
                        + " category, received_by)"
                        + " VALUES (?, '2023-04-22 10:00:00', ?, ?::numeric, 'servicos', ?)"
                        + " RETURNING id",
                Integer.class,
                deviceId,
                paymentType,
                value,
                receivedBy);
    }

    public int customerContact(
            int deviceId, int technicianId, String lastContact, String deviceStatus) {
        return jdbc.queryForObject(
                """
                INSERT INTO customer_contact (id_device, id_technician, phone, type,
                    has_made_contact, last_contact, conversation, device_status)
                VALUES (?, ?, '9876543210', 'ligacao', false, ?::timestamp,
                    'Previous contact attempt', ?)
                RETURNING id
                """,
                Integer.class,
                deviceId,
                technicianId,
                lastContact,
                deviceStatus);
    }
}
