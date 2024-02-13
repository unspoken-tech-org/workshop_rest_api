package com.tproject.workshop.controller;

import com.tproject.workshop.dto.customer.InputCustomerDto;
import com.tproject.workshop.model.Customer;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface CustomerController {

    @GetMapping
    List<Customer> list();

    @GetMapping("/{id}")
    Customer findById(@PathVariable("id") int idCostumer);

    @GetMapping(path = "/find/{name}")
    List<Customer> findByName(@PathVariable("name") String name);

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Customer create(@RequestBody @NotNull InputCustomerDto inputCustomerDto);
}
