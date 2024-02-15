package com.tproject.workshop.integration.controller;


import com.tproject.workshop.integration.AbstractIntegrationLiveTest;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({"/test-scripts/cleanTestData.sql", "/test-scripts/CustomerControllerIT.script.sql"})
public class CustomerControllerIT extends AbstractIntegrationLiveTest {
    private static final String BASE_PATH = "/v1/customer";

    @Order(1)
    @DisplayName("List customers")
    @MethodSource("listCustomersArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void listCustomers(int index, Integer statusCode, Map<String, Object> params, String reason) {
        Response response = given().spec(SPEC)
                .when()
                .get(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }

    private static Stream<Arguments> listCustomersArguments() {
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, Map.of(), "get all Customers")
        );
    }
    @Order(2)
    @DisplayName("find by name")
    @MethodSource("findCustomersArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void findCustomersByName(int index, Integer statusCode, String name, String reason){
        Response response = given().spec(SPEC)
                .when()
                .get(BASE_PATH + "/find/" + name)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }
    public static Stream<Arguments> findCustomersArguments(){
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, "Alfonso", ""),
                Arguments.of(2,HttpStatus.SC_OK,"XXX","")
        );
    }
    @Order(3)
    @DisplayName("find by Id")
    @MethodSource("findCustomersByIdArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void findCustomersById(int index, Integer statusCode, int id, String reason){
        Response response = given().spec(SPEC)
                .when()
                .get(BASE_PATH + "/" + id)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }
    public static Stream<Arguments> findCustomersByIdArguments(){
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, 1, ""),
                Arguments.of(2,HttpStatus.SC_NOT_FOUND,250,"")
        );
    }
    @Order(4)
    @DisplayName("Create Customers")
    @MethodSource("createCustomersArguments")
    @ParameterizedTest(name = "{displayName} : {0} status {1} body {2} reason {3}")
    public void createCustomers(int index, Integer statusCode, Map<String,Object> arguments, String reason){
        Response response = given().spec(SPEC)
                .when()
                .contentType("application/json")
                .body(arguments)
                .post(BASE_PATH)
                .then()
                .statusCode(statusCode)
                .extract()
                .response();

        super.validateResponse(index, response);
    }
    public static Stream<Arguments> createCustomersArguments(){
        return Stream.of(
                Arguments.of(1, HttpStatus.SC_OK, Map.of("name", "generoso", "cpf", "31781477051", "gender", "m", "mail", "geneder@live.com","phone","4499956752","cellphone","5594941236"), ""),
                Arguments.of(2, HttpStatus.SC_BAD_REQUEST, Map.of("name", "generoso", "cpf", "317.814.770-40", "gender", "m", "mail", "geneder@live.com","phone","4499956752","cellphone","5594941236"), "wrong cpf number"),
                Arguments.of(3, HttpStatus.SC_BAD_REQUEST, Map.of("name", "generoso", "cpf", "31781477051", "gender", "m", "mail", "geneder@k.ke.live.com","phone","4499956752","cellphone","5594941236"), "wrong email")

                );
    }
}
