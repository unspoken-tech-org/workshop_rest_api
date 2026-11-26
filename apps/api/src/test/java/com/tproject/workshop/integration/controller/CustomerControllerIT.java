package com.tproject.workshop.integration.controller;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.tproject.workshop.integration.support.AbstractIntegrationTest;
import com.tproject.workshop.support.fixtures.IntegrationFixtures;
import io.restassured.path.json.JsonPath;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

class CustomerControllerIT extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/v1/customer";
    private static final String ALFONSO = "Alfonso Zimmer";
    private static final String JOAO = "João Cláudio da Silva";
    private static final String LUCAS = "Lucas Ribeiro Naga";
    private static final String MARIA = "Maria Oliveira";

    @Autowired private ApplicationContext context;

    private int alfonsoId;
    private int lucasId;
    private int mariaId;
    private int lucasSecondaryPhoneId;

    @BeforeEach
    void seedCustomers() {
        IntegrationFixtures fixtures = new IntegrationFixtures(jdbcTemplate, context);
        fixtures.adminApiKey();
        alfonsoId =
                fixtures.customer(
                        ALFONSO,
                        "31781477051",
                        "masculino",
                        "zimmer@gmail.com",
                        LocalDateTime.of(2021, 4, 26, 8, 41));
        fixtures.customerPhone(alfonsoId, "4430356678", true, "Celular Principal");

        lucasId =
                fixtures.customer(
                        LUCAS,
                        "46203912042",
                        "masculino",
                        "lucas@gmail.com",
                        LocalDateTime.of(2023, 1, 6, 14, 41));
        lucasSecondaryPhoneId = fixtures.customerPhone(lucasId, "44988098766", false, "Trabalho");
        fixtures.customerPhone(lucasId, "44988255540", true, "Luiz");

        mariaId =
                fixtures.customer(
                        MARIA,
                        "98765432100",
                        "feminino",
                        "maria@gmail.com",
                        LocalDateTime.of(2023, 1, 6, 14, 41));
        fixtures.customerPhone(mariaId, "99988887777", true, "Telefone Compartilhado");

        int joaoId =
                fixtures.customer(
                        JOAO,
                        "48541376060",
                        "masculino",
                        "joao@gmail.com",
                        LocalDateTime.of(2025, 1, 6, 14, 41));
        fixtures.customerPhone(joaoId, "48541376060", true, "Celular Principal");
    }

    // ------------------------------------------------------------------ search

    @Test
    @DisplayName("Rejects access without JWT")
    void shouldReturn401_whenNoJwt() {
        given().spec(SPEC)
                .get(BASE_PATH + "/" + alfonsoId)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("Empty search lists every customer ordered by name, with the main phone")
    void shouldListAllCustomers_whenSearchIsEmpty() {
        JsonPath body = search(Map.of(), HttpStatus.OK);

        assertThat(body.getList("content.name", String.class))
                .containsExactly(ALFONSO, JOAO, LUCAS, MARIA);
        assertThat(body.getList("content.mainPhone", String.class))
                .containsExactly("4430356678", "48541376060", "44988255540", "99988887777");
        assertThat(body.getInt("content[0].id")).isEqualTo(alfonsoId);
        assertThat(body.getString("content[0].cpf")).isEqualTo("31781477051");
        assertThat(body.getString("content[0].email")).isEqualTo("zimmer@gmail.com");
        assertThat(body.getString("content[0].gender")).isEqualTo("masculino");
        assertThat(body.getString("content[0].createdAt")).startsWith("2021-04-26T08:41");
        assertThat(body.getInt("totalElements")).isEqualTo(4);
        assertThat(body.getInt("totalPages")).isEqualTo(1);
        assertThat(body.getInt("size")).isEqualTo(15);
        assertThat(body.getBoolean("last")).isTrue();
    }

    @ParameterizedTest(name = "{0}=\"{1}\" -> {2}")
    @CsvSource(
            delimiter = '|',
            value = {
                "searchName|Alfonso|Alfonso Zimmer",
                "searchName|alf|Alfonso Zimmer",
                "searchName|ALF|Alfonso Zimmer",
                "searchName|Zimmer|Alfonso Zimmer",
                "searchName|João|João Cláudio da Silva",
                "searchName|NonExistentName|",
                "cpf|31781477051|Alfonso Zimmer",
                "cpf|317.814.770-51|Alfonso Zimmer",
                "cpf|00000000000|",
                "phone|4430356678|Alfonso Zimmer",
                "phone|44988098766|Lucas Ribeiro Naga",
                "phone|9999999999|",
                // Formatted phones are not normalized by the search (documented behavior).
                "phone|(44) 3035-6678|"
            })
    @DisplayName("Filters customers by name, cpf and phone")
    void shouldFilterCustomers_whenFilterIsGiven(String field, String value, String expected) {
        JsonPath body = search(Map.of(field, value), HttpStatus.OK);

        List<String> expectedNames = expected == null ? List.of() : List.of(expected);
        assertThat(body.getList("content.name", String.class)).isEqualTo(expectedNames);
        assertThat(body.getInt("totalElements")).isEqualTo(expectedNames.size());
    }

    @Test
    @DisplayName("A blank name filter does not restrict the search")
    void shouldListAllCustomers_whenNameFilterIsBlank() {
        JsonPath body = search(Map.of("searchName", ""), HttpStatus.OK);

        assertThat(body.getInt("totalElements")).isEqualTo(4);
    }

    @ParameterizedTest(name = "blank {0} matches nothing")
    @ValueSource(strings = {"cpf", "phone"})
    @DisplayName("Blank cpf and phone filters match no customer (current behavior)")
    void shouldReturnEmptyPage_whenCpfOrPhoneFilterIsBlank(String field) {
        JsonPath body = search(Map.of(field, ""), HttpStatus.OK);

        assertThat(body.getList("content")).isEmpty();
        assertThat(body.getInt("totalElements")).isZero();
    }

    @Test
    @DisplayName("Filters customers by id")
    void shouldFindCustomer_whenSearchingById() {
        JsonPath found = search(Map.of("id", lucasId), HttpStatus.OK);
        JsonPath missing = search(Map.of("id", lucasId + 9999), HttpStatus.OK);

        assertThat(found.getList("content.id", Integer.class)).containsExactly(lucasId);
        assertThat(missing.getList("content")).isEmpty();
        assertThat(missing.getInt("totalElements")).isZero();
    }

    @Test
    @DisplayName("Paginates results")
    void shouldPaginate_whenPageAndSizeAreGiven() {
        JsonPath first = search(Map.of("page", 0, "size", 2), HttpStatus.OK);
        JsonPath single = search(Map.of("page", 0, "size", 1), HttpStatus.OK);
        JsonPath beyond = search(Map.of("page", 10, "size", 5), HttpStatus.OK);

        assertThat(first.getList("content.name", String.class)).containsExactly(ALFONSO, JOAO);
        assertThat(first.getInt("totalElements")).isEqualTo(4);
        assertThat(first.getInt("totalPages")).isEqualTo(2);
        assertThat(first.getBoolean("last")).isFalse();
        assertThat(single.getInt("totalPages")).isEqualTo(4);
        assertThat(single.getList("content.name", String.class)).containsExactly(ALFONSO);
        assertThat(beyond.getList("content")).isEmpty();
        assertThat(beyond.getInt("totalElements")).isEqualTo(4);
        assertThat(beyond.getBoolean("last")).isTrue();
    }

    @ParameterizedTest(name = "page {0}, size {1} -> {2}")
    @CsvSource(
            delimiter = '|',
            value = {
                "0|0|O tamanho da página deve ser maior que zero",
                "-1|5|O número da página deve ser maior ou igual a zero",
                "0|1000|O tamanho da página deve ser no máximo 100"
            })
    @DisplayName("Rejects invalid pagination")
    void shouldReturn400_whenPaginationIsInvalid(int page, int size, String detail) {
        JsonPath body = search(Map.of("page", page, "size", size), HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail")).isEqualTo(detail);
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "|empty string body",
                "null|null literal body",
                "{searchName: \"test\"}|malformed JSON (unquoted key)",
                "{\"page\": \"abc\"}|string instead of integer for page",
                "{\"size\": -1}|negative size"
            })
    @DisplayName("Rejects an invalid request body")
    void shouldReturn400_whenSearchBodyIsInvalid(String body, String reason) {
        given().spec(getAuthenticatedSpec())
                .body(body == null ? "" : body)
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("Rejects a search without body")
    void shouldReturn400_whenSearchHasNoBody() {
        given().spec(getAuthenticatedSpec())
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    // ------------------------------------------------------------------ detail

    @Test
    @DisplayName("Finds a customer by id with phones")
    void shouldReturnCustomer_whenIdExists() {
        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH + "/" + alfonsoId)
                        .then()
                        .statusCode(HttpStatus.OK.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getInt("customerId")).isEqualTo(alfonsoId);
        assertThat(body.getString("name")).isEqualTo(ALFONSO);
        assertThat(body.getString("cpf")).isEqualTo("31781477051");
        assertThat(body.getString("gender")).isEqualTo("masculino");
        assertThat(body.getString("email")).isEqualTo("zimmer@gmail.com");
        assertThat(phones(body)).containsExactly(tuple("4430356678", "Celular Principal", true));
        assertThat(body.getList("customerDevices")).isEmpty();
    }

    @Test
    @DisplayName("Returns 404 for an unknown customer")
    void shouldReturn404_whenCustomerDoesNotExist() {
        int unknown = alfonsoId + 9999;

        JsonPath body =
                given().spec(getAuthenticatedSpec())
                        .get(BASE_PATH + "/" + unknown)
                        .then()
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .extract()
                        .jsonPath();

        assertThat(body.getString("code")).isEqualTo("entity.not.found.for.request");
        assertThat(body.getString("detail"))
                .isEqualTo("Cliente com id " + unknown + " não encontrado");
    }

    // ------------------------------------------------------------------ create

    @Test
    @DisplayName("Creates a customer with new phones")
    void shouldCreateCustomer_whenPhonesAreNew() {
        JsonPath body =
                create(
                        customerBody(
                                "João da Silva",
                                "753.865.440-20",
                                "masculino",
                                "joao@example.com",
                                phone("11999991111", "Celular Principal", true),
                                phone("11555552222", "Trabalho", false)),
                        HttpStatus.CREATED);

        assertThat(body.getInt("customerId")).isNotIn(alfonsoId, lucasId, mariaId);
        assertThat(body.getString("name")).isEqualTo("João da Silva");
        assertThat(body.getString("cpf")).isEqualTo("75386544020");
        assertThat(body.getString("gender")).isEqualTo("masculino");
        assertThat(body.getString("createdAt")).isNotBlank();
        assertThat(phones(body))
                .containsExactlyInAnyOrder(
                        tuple("11999991111", "Celular Principal", true),
                        tuple("11555552222", "Trabalho", false));
        assertThat(countCustomers()).isEqualTo(5);
    }

    @Test
    @DisplayName("Creating a customer reuses an existing phone")
    void shouldReuseExistingPhone_whenNumberAlreadyRegistered() {
        JsonPath body =
                create(
                        customerBody(
                                "Maria Oliveira Santos",
                                "753.865.440-20",
                                "feminino",
                                "maria@example.com",
                                phone("44900998877", "Pessoal", true),
                                phone("44988098766", null, false)),
                        HttpStatus.CREATED);

        List<Map<String, Object>> phones = body.getList("phones");
        Map<String, Object> reused =
                phones.stream()
                        .filter(p -> "44988098766".equals(p.get("number")))
                        .findFirst()
                        .orElseThrow();
        assertThat(((Number) reused.get("id")).intValue()).isEqualTo(lucasSecondaryPhoneId);
        assertThat(reused.get("main")).isEqualTo(false);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM phones WHERE number = '44988098766'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Rejects two primary phones")
    void shouldReturn400_whenCreatingWithTwoPrimaryPhones() {
        JsonPath body =
                create(
                        customerBody(
                                "Carlos Souza",
                                "753.865.440-20",
                                "masculino",
                                "carlos@example.com",
                                phone("21987654321", "Principal 1", true),
                                phone("21912345678", "Principal 2", true)),
                        HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("requisicao.invalida");
        assertThat(body.getString("detail"))
                .isEqualTo("Não é possível ter mais de um telefone primário");
        assertThat(countCustomers()).isEqualTo(4);
    }

    @Test
    @DisplayName("Rejects an invalid CPF")
    void shouldReturn400_whenCreatingWithInvalidCpf() {
        JsonPath body =
                create(
                        customerBody(
                                "Cliente Teste",
                                "317.814.770-40",
                                "masculino",
                                "teste@example.com",
                                phone("1199999999", "Teste", true)),
                        HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail")).isEqualTo("Número de CPF inválido");
    }

    @Test
    @DisplayName("Rejects a blank name")
    void shouldReturn400_whenCreatingWithoutName() {
        Map<String, Object> request =
                customerBody(
                        "x",
                        "753.865.440-20",
                        "masculino",
                        "teste@example.com",
                        phone("1199999999", "Teste", true));
        request.remove("name");

        JsonPath body = create(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("erro.validacao");
        assertThat(body.getString("detail")).isEqualTo("must not be blank");
    }

    @Test
    @DisplayName("Rejects a customer without phones")
    void shouldReturn400_whenCreatingWithoutPhones() {
        Map<String, Object> request =
                customerBody("Cliente Teste", "753.865.440-20", "masculino", "teste@example.com");
        request.remove("phones");

        JsonPath body = create(request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail")).isEqualTo("must not be empty");
    }

    @Test
    @DisplayName("Rejects a CPF already in use")
    void shouldReturn409_whenCpfAlreadyExists() {
        JsonPath body =
                create(
                        customerBody(
                                "Cliente com CPF Duplicado",
                                "31781477051",
                                "masculino",
                                "duplicado@example.com",
                                phone("1199999999", "Teste", true)),
                        HttpStatus.CONFLICT);

        assertThat(body.getString("code")).isEqualTo("recurso.conflito");
        assertThat(body.getString("detail")).isEqualTo("O CPF informado já está em uso");
        assertThat(countCustomers()).isEqualTo(4);
    }

    @Test
    @DisplayName("Rejects a primary phone that is primary for another customer")
    void shouldReturn409_whenPrimaryPhoneBelongsToAnotherCustomer() {
        JsonPath body =
                create(
                        customerBody(
                                "Cliente com Telefone Principal Duplicado",
                                "753.865.440-20",
                                "masculino",
                                "telefone_duplicado@example.com",
                                phone("4430356678", "Teste", true)),
                        HttpStatus.CONFLICT);

        assertThat(body.getString("code")).isEqualTo("recurso.conflito");
        assertThat(body.getString("detail"))
                .isEqualTo(
                        "O número informado já está cadastrado como principal para outro cliente");
        assertThat(countCustomers()).isEqualTo(4);
    }

    // ------------------------------------------------------------------ update

    @Test
    @DisplayName("Changes the primary phone")
    void shouldChangePrimaryPhone_whenUpdating() {
        JsonPath body =
                update(
                        alfonsoId,
                        alfonso(
                                phone("4430356678", "Celular Principal", false),
                                phone("11555552222", "Novo Principal", true)),
                        HttpStatus.OK);

        assertThat(body.getInt("customerId")).isEqualTo(alfonsoId);
        assertThat(phones(body))
                .containsExactlyInAnyOrder(
                        tuple("4430356678", "Celular Principal", false),
                        tuple("11555552222", "Novo Principal", true));
    }

    @Test
    @DisplayName("Adds a secondary phone")
    void shouldAddPhone_whenUpdating() {
        JsonPath body =
                update(
                        alfonsoId,
                        alfonso(
                                phone("4430356678", "Celular Principal", true),
                                phone("11933334444", "Recados", false)),
                        HttpStatus.OK);

        assertThat(phones(body))
                .containsExactlyInAnyOrder(
                        tuple("4430356678", "Celular Principal", true),
                        tuple("11933334444", "Recados", false));
    }

    @Test
    @DisplayName("Removes a secondary phone")
    void shouldRemovePhone_whenItIsOmittedFromUpdate() {
        JsonPath body =
                update(
                        lucasId,
                        customerBody(
                                LUCAS,
                                "46203912042",
                                "masculino",
                                "lucas@gmail.com",
                                phone("44988255540", "Luiz", true)),
                        HttpStatus.OK);

        assertThat(phones(body)).containsExactly(tuple("44988255540", "Luiz", true));
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM customer_phones WHERE id_customer = ?",
                                Integer.class,
                                lucasId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Updates a phone alias")
    void shouldUpdatePhoneAlias_whenUpdating() {
        JsonPath body =
                update(
                        mariaId,
                        customerBody(
                                MARIA,
                                "98765432100",
                                "feminino",
                                "maria@gmail.com",
                                phone("99988887777", "Comercial", true)),
                        HttpStatus.OK);

        assertThat(phones(body)).containsExactly(tuple("99988887777", "Comercial", true));
    }

    @Test
    @DisplayName("Keeps the same CPF while changing other fields")
    void shouldUpdateCustomer_whenCpfIsUnchanged() {
        JsonPath body =
                update(
                        alfonsoId,
                        customerBody(
                                "Alfonso Zimmer Atualizado",
                                "31781477051",
                                "masculino",
                                "zimmer_novo@gmail.com",
                                phone("4430356678", "Celular Principal Atualizado", true)),
                        HttpStatus.OK);

        assertThat(body.getString("name")).isEqualTo("Alfonso Zimmer Atualizado");
        assertThat(body.getString("email")).isEqualTo("zimmer_novo@gmail.com");
        assertThat(body.getString("createdAt")).startsWith("2021-04-26T08:41");
        assertThat(phones(body))
                .containsExactly(tuple("4430356678", "Celular Principal Atualizado", true));
    }

    @Test
    @DisplayName("Rejects two primary phones on update")
    void shouldReturn400_whenUpdatingWithTwoPrimaryPhones() {
        JsonPath body =
                update(
                        alfonsoId,
                        alfonso(
                                phone("4430356678", "Principal 1", true),
                                phone("11999998888", "Principal 2", true)),
                        HttpStatus.BAD_REQUEST);

        assertThat(body.getString("code")).isEqualTo("requisicao.invalida");
        assertThat(body.getString("detail"))
                .isEqualTo("Não é possível ter mais de um telefone primário");
    }

    @Test
    @DisplayName("Returns 404 when updating an unknown customer")
    void shouldReturn404_whenUpdatingUnknownCustomer() {
        int unknown = alfonsoId + 9999;

        JsonPath body =
                update(
                        unknown,
                        customerBody(
                                "Cliente Inexistente",
                                "45931651055",
                                "masculino",
                                "inexistente@example.com",
                                phone("1199999999", "Teste", true)),
                        HttpStatus.NOT_FOUND);

        assertThat(body.getString("detail"))
                .isEqualTo("Cliente com id " + unknown + " não encontrado");
    }

    @Test
    @DisplayName("Rejects an invalid CPF on update")
    void shouldReturn400_whenUpdatingWithInvalidCpf() {
        Map<String, Object> request =
                customerBody(
                        ALFONSO,
                        "11111111111",
                        "masculino",
                        "zimmer@gmail.com",
                        phone("4430356678", "Teste", true));

        JsonPath body = update(alfonsoId, request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail")).isEqualTo("Número de CPF inválido");
    }

    @Test
    @DisplayName("Rejects a blank name on update")
    void shouldReturn400_whenUpdatingWithoutName() {
        Map<String, Object> request = alfonso(phone("4430356678", "Teste", true));
        request.remove("name");

        JsonPath body = update(alfonsoId, request, HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail")).isEqualTo("must not be blank");
    }

    @Test
    @DisplayName("Rejects duplicate phone numbers in the same request")
    void shouldReturn400_whenUpdatingWithDuplicatePhoneNumbers() {
        JsonPath body =
                update(
                        alfonsoId,
                        alfonso(
                                phone("4430356678", "Teste", true),
                                phone("4430356678", "Teste 2", false)),
                        HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo("Não é possível ter telefones com números iguais");
    }

    @Test
    @DisplayName("Rejects a phone number with the wrong length")
    void shouldReturn400_whenUpdatingWithInvalidPhoneNumber() {
        JsonPath body =
                update(
                        alfonsoId,
                        alfonso(phone("449995675", "Inválido", true)),
                        HttpStatus.BAD_REQUEST);

        assertThat(body.getString("detail"))
                .isEqualTo("Número de telefone deve ter entre 10 e 11 dígitos");
    }

    @Test
    @DisplayName("Rejects a CPF that belongs to another customer")
    void shouldReturn409_whenUpdatingWithCpfOfAnotherCustomer() {
        Map<String, Object> request = alfonso(phone("4430356678", "Teste", true));
        request.put("cpf", "46203912042");

        JsonPath body = update(alfonsoId, request, HttpStatus.CONFLICT);

        assertThat(body.getString("code")).isEqualTo("recurso.conflito");
        assertThat(body.getString("detail")).isEqualTo("O CPF informado já está em uso");
    }

    // ----------------------------------------------------------------- helpers

    private JsonPath search(Map<String, Object> params, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(params)
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private JsonPath create(Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .post(BASE_PATH)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private JsonPath update(int id, Map<String, Object> body, HttpStatus expected) {
        return given().spec(getAuthenticatedSpec())
                .body(body)
                .put(BASE_PATH + "/{id}", id)
                .then()
                .statusCode(expected.value())
                .extract()
                .jsonPath();
    }

    private int countCustomers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
    }

    private static List<org.assertj.core.groups.Tuple> phones(JsonPath body) {
        List<Map<String, Object>> phones = body.getList("phones");
        return phones.stream()
                .map(p -> tuple(p.get("number"), p.get("name"), p.get("main")))
                .toList();
    }

    private static Map<String, Object> alfonso(Map<String, Object>... phones) {
        return customerBody(ALFONSO, "31781477051", "masculino", "zimmer@gmail.com", phones);
    }

    @SafeVarargs
    private static Map<String, Object> customerBody(
            String name, String cpf, String gender, String email, Map<String, Object>... phones) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("cpf", cpf);
        body.put("gender", gender);
        body.put("email", email);
        if (phones.length > 0) {
            body.put("phones", new ArrayList<>(List.of(phones)));
        }
        return body;
    }

    private static Map<String, Object> phone(String number, String name, boolean primary) {
        Map<String, Object> phone = new HashMap<>();
        phone.put("number", number);
        if (name != null) {
            phone.put("name", name);
        }
        phone.put("isPrimary", primary);
        return phone;
    }
}
