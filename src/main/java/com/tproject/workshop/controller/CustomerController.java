package com.tproject.workshop.controller;

import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

public interface CustomerController {

    @GetMapping("/{id}")
    CustomerOutputDto findById(@PathVariable("id") int idCostumer);

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CustomerOutputDto create(@RequestBody @NotNull @Valid InputCustomerDto inputCustomerDto);

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    CustomerOutputDto update(@PathVariable("id") int id, @RequestBody @NotNull @Valid InputCustomerDto inputCustomerDto);
}
