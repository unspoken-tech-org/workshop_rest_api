package com.tproject.workshop.integration.controller;


import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/AuthSetup.sql", "/test-scripts/CustomerControllerIT.script.sql", "/test-scripts/resetTablesSequence.sql"})
public class CustomerControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/customer";

    @Order(0)
    @DisplayName("Reject access without JWT")
    @Test
    public void shouldReturn401_whenNoJwt() {
        given().spec(SPEC)
                .when()
                .get(BASE_PATH + "/1")
                .then()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Order(1)
    @DisplayName("Search customers")
    @MethodSource("searchCustomersArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void searchCustomers(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .body(params)
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> searchCustomersArguments() {
        return Stream.of(
                // Basic search tests
                Arguments.of(1, HttpStatus.SC_OK, Map.of(), "get all customers"),
                Arguments.of(2, HttpStatus.SC_OK, Map.of("name", "Alfonso"), "get customer by name"),
                Arguments.of(3, HttpStatus.SC_OK, Map.of("cpf", "31781477051"), "get customer by cpf"),
                Arguments.of(4, HttpStatus.SC_OK, Map.of("phone", "4430356678"), "get customer by phone"),
                Arguments.of(5, HttpStatus.SC_OK, Map.of("id", 1), "get customer by id"),

                // Pagination tests
                Arguments.of(6, HttpStatus.SC_OK, Map.of("page", 0, "size", 2), "pagination first page with size 2"),
                Arguments.of(7, HttpStatus.SC_OK, Map.of("page", 2, "size", 2), "pagination second page with size 2"),
                Arguments.of(8, HttpStatus.SC_OK, Map.of("page", 0, "size", 1), "pagination with size 1"),
                Arguments.of(9, HttpStatus.SC_OK, Map.of("page", 0, "size", 10), "pagination with large page size"),

                // Partial matches and case-insensitive
                Arguments.of(10, HttpStatus.SC_OK, Map.of("name", "alf"), "partial name search lowercase"),
                Arguments.of(11, HttpStatus.SC_OK, Map.of("name", "ALF"), "partial name search uppercase"),
                Arguments.of(12, HttpStatus.SC_OK, Map.of("name", "Zimmer"), "search by last name"),

                // Edge cases - no results
                Arguments.of(13, HttpStatus.SC_OK, Map.of("name", "NonExistentName"), "search with non-existent name"),
                Arguments.of(14, HttpStatus.SC_OK, Map.of("cpf", "00000000000"), "search with non-existent cpf"),
                Arguments.of(15, HttpStatus.SC_OK, Map.of("phone", "9999999999"), "search with non-existent phone"),
                Arguments.of(16, HttpStatus.SC_OK, Map.of("id", 9999), "search with non-existent id"),

                // Empty/null values
                Arguments.of(17, HttpStatus.SC_OK, Map.of("name", ""), "search with empty name"),
                Arguments.of(18, HttpStatus.SC_OK, Map.of("cpf", ""), "search with empty cpf"),
                Arguments.of(19, HttpStatus.SC_OK, Map.of("phone", ""), "search with empty phone"),

                // Special characters and formatting
                Arguments.of(20, HttpStatus.SC_OK, Map.of("cpf", "317.814.770-51"), "search with formatted cpf"),
                Arguments.of(21, HttpStatus.SC_OK, Map.of("cpf", "31781477051"), "search with unformatted cpf"),

                Arguments.of(22, HttpStatus.SC_OK, Map.of("phone", "(44) 3035-6678"), "search with formatted phone"),
                Arguments.of(23, HttpStatus.SC_OK, Map.of("phone", "4430356678"), "search with unformatted phone"),
                Arguments.of(24, HttpStatus.SC_OK, Map.of(
                        "phone", "44988098766" //Lucas secondary phone
                ), "search by secondary phone"),
                Arguments.of(25, HttpStatus.SC_OK, Map.of("name", "João"), "search with accented characters"),


                // Complex pagination scenarios
                Arguments.of(26, HttpStatus.SC_OK, Map.of("page", 10, "size", 5), "pagination beyond available data"),
                Arguments.of(27, HttpStatus.SC_BAD_REQUEST, Map.of("page", 0, "size", 0), "pagination with zero size"),
                Arguments.of(28, HttpStatus.SC_BAD_REQUEST, Map.of("page", -1, "size", 5), "pagination with negative page"),

                // Large page size edge case
                Arguments.of(29, HttpStatus.SC_OK, Map.of("page", 0, "size", 1000), "pagination with very large page size")
        );
    }

    @Order(2)
    @DisplayName("find by Id")
    @MethodSource("findCustomersByIdArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void findCustomersById(int index, Integer statusCode, int id, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .get(BASE_PATH + "/" + id)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    public static Stream<Arguments> findCustomersByIdArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, 1, ""),
                Arguments.of(2, HttpStatus.SC_NOT_FOUND, 250, "")
        );
    }

    @Order(3)
    @DisplayName("Create Customers")
    @MethodSource("createCustomersArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void createCustomers(int index, Integer statusCode, Map<String, Object> arguments, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .contentType("application/json")
                .body(arguments)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes(index, response, List.of("insertDate"));
    }

    public static Stream<Arguments> createCustomersArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_CREATED, Map.of(
                        "name", "João da Silva",
                        "cpf", "753.865.440-20",
                        "gender", "masculino",
                        "email", "joao@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "11999991111",
                                        "name", "Celular Principal",
                                        "isPrimary", true
                                ),
                                Map.of(
                                        "number", "11555552222",
                                        "name", "Trabalho",
                                        "isPrimary", false
                                )
                        )
                ), "Successful customer registration (new phones)"),

                Arguments.of(2, HttpStatus.SC_CREATED, Map.of(
                        "name", "Maria Oliveira Santos",
                        "cpf", "753.865.440-20",
                        "gender", "feminino",
                        "email", "maria@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "44900998877",
                                        "name", "Pessoal",
                                        "isPrimary", true
                                ),
                                Map.of(
                                        "number", "44988098766", // Phone already exists as secondary in the database
                                        "isPrimary", false
                                )
                        )
                ), "Customer registration reusing an existing phone"),

                Arguments.of(3, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "name", "Carlos Souza",
                        "cpf", "753.865.440-20",
                        "gender", "masculino",
                        "email", "carlos@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "21987654321",
                                        "name", "Principal 1",
                                        "isPrimary", true
                                ),
                                Map.of(
                                        "number", "21912345678",
                                        "name", "Principal 2",
                                        "isPrimary", true
                                )
                        )
                ), "Failure when trying to register customer with two primary phones"),

                Arguments.of(4, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "name", "Cliente Teste",
                        "cpf", "317.814.770-40", // Invalid CPF
                        "gender", "masculino",
                        "email", "teste@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "1199999999",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "Invalid CPF"),

                Arguments.of(5, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "cpf", "753.865.440-20", // Blank name
                        "gender", "masculino",
                        "email", "teste@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "1199999999",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "Blank name"),

                Arguments.of(6, HttpStatus.SC_BAD_REQUEST, Map.of(
                        "name", "Cliente Teste",
                        "cpf", "753.865.440-20",
                        "gender", "masculino",
                        "email", "teste@example.com"
                ), "Empty phone list"),

                Arguments.of(7, HttpStatus.SC_CONFLICT, Map.of(
                        "name", "Cliente com CPF Duplicado",
                        "cpf", "31781477051", // CPF already exists (Alfonso Zimmer's)
                        "gender", "masculino",
                        "email", "duplicado@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "1199999999",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "CPF already exists"),

                Arguments.of(8, HttpStatus.SC_CONFLICT, Map.of(
                        "name", "Cliente com Telefone Principal Duplicado",
                        "cpf", "753.865.440-20",
                        "gender", "masculino",
                        "email", "telefone_duplicado@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678", // Phone already exists (Alfonso Zimmer's)
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "Primary phone already exists for another customer")
        );
    }

    @Order(4)
    @DisplayName("Update Customers")
    @MethodSource("updateCustomersArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void updateCustomers(int index, Integer statusCode, Integer id, Map<String, Object> arguments, String reason) {
        Response response = given().spec(getAuthenticatedSpec())
                .when()
                .contentType("application/json")
                .body(arguments)
                .put(BASE_PATH + "/{id}", id)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponseIgnoreAttributes(index, response, List.of("insertDate"));
    }

    public static Stream<Arguments> updateCustomersArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "31781477051",
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Celular Principal",
                                        "isPrimary", false // Was primary, now is secondary
                                ),
                                Map.of(
                                        "number", "11555552222",
                                        "name", "Novo Principal",
                                        "isPrimary", true // New primary phone
                                )
                        )
                ), "Change the primary phone"),

                Arguments.of(2, HttpStatus.SC_OK, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "31781477051",
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Celular Principal",
                                        "isPrimary", true
                                ),
                                Map.of(
                                        "number", "11933334444",
                                        "name", "Recados",
                                        "isPrimary", false // New secondary phone
                                )
                        )
                ), "Add a new phone to an existing customer"),

                Arguments.of(3, HttpStatus.SC_OK, 2, Map.of(
                        "name", "Lucas Ribeiro Naga",
                        "cpf", "46203912042",
                        "gender", "masculino",
                        "email", "lucas@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "44988255540", // Keep only the primary
                                        "name", "Luiz",
                                        "isPrimary", true
                                )
                                // Remove the secondary phone 44988098766
                        )
                ), "Remove a secondary phone from a customer"),

                Arguments.of(4, HttpStatus.SC_OK, 3, Map.of(
                        "name", "Maria Oliveira",
                        "cpf", "98765432100",
                        "gender", "feminino",
                        "email", "maria@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "99988887777",
                                        "name", "Comercial", // Changing "Shared Phone" to "Comercial"
                                        "isPrimary", true
                                )
                        )
                ), "Update phone alias"),

                Arguments.of(5, HttpStatus.SC_BAD_REQUEST, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "31781477051",
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Principal 1",
                                        "isPrimary", true
                                ),
                                Map.of(
                                        "number", "11999998888",
                                        "name", "Principal 2",
                                        "isPrimary", true // Two primary phones - should give an error
                                )
                        )
                ), "Failure when trying to assign two primary phones in update"),

                Arguments.of(6, HttpStatus.SC_NOT_FOUND, 250, Map.of(
                        "name", "Cliente Inexistente",
                        "cpf", "45931651055",
                        "gender", "masculino",
                        "email", "inexistente@example.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "1199999999",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "Customer not found"),

                Arguments.of(7, HttpStatus.SC_BAD_REQUEST, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "11111111111", // Invalid CPF
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "Invalid CPF in update"),

                Arguments.of(8, HttpStatus.SC_BAD_REQUEST, 1, Map.of(
                        "cpf", "31781477051", // Blank name
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "Blank name in update"),

                Arguments.of(9, HttpStatus.SC_BAD_REQUEST, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "31781477051",
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Teste",
                                        "isPrimary", true
                                ),
                                Map.of(
                                        "number", "4430356678", // Duplicate number in the same request
                                        "name", "Teste 2",
                                        "isPrimary", false
                                )
                        )
                ), "Duplicate phone numbers in the same request"),

                Arguments.of(10, HttpStatus.SC_BAD_REQUEST, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "31781477051",
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "449995675", // Too short number format
                                        "name", "Inválido",
                                        "isPrimary", true
                                )
                        )
                ), "Invalid phone number"),

                Arguments.of(11, HttpStatus.SC_OK, 1, Map.of(
                        "name", "Alfonso Zimmer Atualizado",
                        "cpf", "31781477051", // Same CPF, should work
                        "gender", "masculino",
                        "email", "zimmer_novo@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Celular Principal Atualizado",
                                        "isPrimary", true
                                )
                        )
                ), "Update with same CPF should work"),

                Arguments.of(12, HttpStatus.SC_CONFLICT, 1, Map.of(
                        "name", "Alfonso Zimmer",
                        "cpf", "46203912042", // CPF of Lucas - should give error
                        "gender", "masculino",
                        "email", "zimmer@gmail.com",
                        "phones", List.of(
                                Map.of(
                                        "number", "4430356678",
                                        "name", "Teste",
                                        "isPrimary", true
                                )
                        )
                ), "CPF already exists for another customer")
        );
    }
}