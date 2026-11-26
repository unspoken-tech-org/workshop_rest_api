package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PaymentIdempotencyIT extends AbstractIntegrationTest {

    private static final String PATH = "/v1/payment";
    private static final String HEADER = "Idempotency-Key";

    private int deviceId;

    @BeforeEach
    void prepareData() {
        jdbcTemplate.update(
                """
                INSERT INTO api_keys (key_value, client_name, user_identifier, platform, role,
                                      description, active, created_at)
                VALUES (encode(sha256(convert_to(?, 'UTF8')), 'hex'), 'idem_app', 'idem_user',
                        'MOBILE', 'ADMIN', 'idempotency IT', true, CURRENT_TIMESTAMP)
                """,
                API_KEY);
        int customer =
                insert(
                        "INSERT INTO customers(name, created_at, cpf, gender, email)"
                                + " VALUES ('Cliente Idem', now(), '31781477051', 'm', 'a@b.com')"
                                + " RETURNING id");
        int technician =
                insert(
                        "INSERT INTO technicians(technician_name, number)"
                                + " VALUES ('Tec', '44988254493') RETURNING id");
        int brand = insert("INSERT INTO brands(brand) VALUES ('Marca') RETURNING id");
        int model = insert("INSERT INTO models(model) VALUES ('Modelo') RETURNING id");
        int type = insert("INSERT INTO types(type) VALUES ('Tipo') RETURNING id");
        int bmt =
                jdbcTemplate.queryForObject(
                        "INSERT INTO brands_models_types(id_brand, id_model, id_type)"
                                + " VALUES (?, ?, ?) RETURNING id",
                        Integer.class,
                        brand,
                        model,
                        type);
        deviceId =
                jdbcTemplate.queryForObject(
                        """
                        INSERT INTO devices (id_customer, id_brand_model_type, device_status,
                            id_technician, entry_date, problem, budget_fee, service_value,
                            has_urgency, is_revision, color_ids)
                        VALUES (?, ?, 'EM_ANDAMENTO', ?, '2023-04-10 08:41:00', 'Problem',
                            50.00, 5000.00, false, false, '{}'::int[])
                        RETURNING id
                        """,
                        Integer.class,
                        customer,
                        bmt,
                        technician);
    }

    @Test
    @DisplayName("Same key and same body creates one payment and replays the same response")
    void shouldReplayResponse_whenSameKeyAndSameBody() {
        String key = UUID.randomUUID().toString();

        Response first = post(key, body(150.00));
        Response second = post(key, body(150.00));

        assertThat(first.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(second.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(second.getBody().asString()).isEqualTo(first.getBody().asString());
        assertThat(first.getHeader("Idempotent-Replayed")).isNull();
        assertThat(second.getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Key order and whitespace in the body do not count as a different body")
    void shouldReplay_whenBodyDiffersOnlyInFormatting() {
        String key = UUID.randomUUID().toString();
        String reordered =
                "{\"receivedBy\":\"Joao\",\"category\":\"servicos\",\"value\":150.00,"
                        + "\"paymentType\":\"credito\",\"deviceId\":"
                        + deviceId
                        + "}";

        Response first = post(key, body(150.00));
        Response second = post(key, reordered);

        assertThat(second.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(second.getBody().asString()).isEqualTo(first.getBody().asString());
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Same key with a different body returns 422 and creates nothing new")
    void shouldReturn422_whenSameKeyAndDifferentBody() {
        String key = UUID.randomUUID().toString();

        post(key, body(150.00));
        Response second = post(key, body(99.00));

        assertThat(second.statusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(second.jsonPath().getString("code")).isEqualTo("idempotency.key.reused");
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Without the header every request creates a payment, as before")
    void shouldCreateEachPayment_whenNoKey() {
        Response first = post(null, body(150.00));
        Response second = post(null, body(150.00));

        assertThat(first.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(second.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(second.jsonPath().getInt("id")).isNotEqualTo(first.jsonPath().getInt("id"));
        assertThat(paymentCount()).isEqualTo(2);
        assertThat(count("idempotency_keys")).isZero();
    }

    @Test
    @DisplayName("A failed request does not keep the key: the client can retry with it")
    void shouldAllowRetry_whenFirstAttemptFailed() {
        String key = UUID.randomUUID().toString();

        Response invalid = post(key, body(-1.00));
        Response retry = post(key, body(150.00));

        assertThat(invalid.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(retry.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("An expired key is accepted again as a new request")
    void shouldCreateNewPayment_whenKeyExpired() {
        String key = UUID.randomUUID().toString();

        post(key, body(150.00));
        jdbcTemplate.update("UPDATE idempotency_keys SET expires_at = now() - interval '1 second'");
        Response second = post(key, body(99.00));

        assertThat(second.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(paymentCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Malformed key returns 400")
    void shouldReturn400_whenKeyIsInvalid() {
        Response tooLong = post("k".repeat(256), body(150.00));
        Response withSpace = post("has space", body(150.00));

        assertThat(tooLong.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(withSpace.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(paymentCount()).isZero();
    }

    @Test
    @DisplayName("Concurrent requests with the same key create exactly one payment")
    void shouldCreateOnePayment_whenRequestsAreConcurrent() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = body(150.00);
        int threads = 8;
        getAuthenticatedSpec();

        List<Response> responses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Response>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Callable<Response> task =
                        () -> {
                            start.await();
                            return post(key, body);
                        };
                futures.add(pool.submit(task));
            }
            start.countDown();
            for (Future<Response> future : futures) {
                responses.add(future.get());
            }
        }

        List<Response> created =
                responses.stream()
                        .filter(r -> r.statusCode() == HttpStatus.CREATED.value())
                        .toList();
        assertThat(created).isNotEmpty();
        assertThat(responses)
                .allMatch(r ->
                        r.statusCode() == HttpStatus.CREATED.value()
                                || r.statusCode() == HttpStatus.CONFLICT.value());
        assertThat(created)
                .map(r -> r.getBody().asString())
                .containsOnly(created.get(0).getBody().asString());
        assertThat(paymentCount()).isEqualTo(1);
    }

    private Response post(String key, Object body) {
        var request = given().spec(getAuthenticatedSpec());
        if (key != null) {
            request = request.header(HEADER, key);
        }
        return request.body(body).when().post(PATH).then().extract().response();
    }

    private String body(double value) {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .valueToTree(
                        Map.of(
                                "deviceId", deviceId,
                                "paymentType", "credito",
                                "value", value,
                                "category", "servicos",
                                "receivedBy", "Joao"))
                .toString();
    }

    private int paymentCount() {
        return count("payments");
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private int insert(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
