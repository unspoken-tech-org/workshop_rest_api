package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.CustomerController;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDto;
import com.tproject.workshop.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/customer")
public class CustomerControllerImpl implements CustomerController {

    private final CustomerService customerService;

    @Override
    public CustomerOutputDto findById(int id) {
        return customerService.findById(id);
    }

    @Override
    public CustomerOutputDto create(@Valid InputCustomerDto inputCustomerDto) {
        return customerService.saveCustomer(inputCustomerDto);
    }

    @Override
    public CustomerOutputDto update(int id, InputCustomerDto inputCustomerDto) {
        return customerService.updateCustomer(id, inputCustomerDto);
    }
}
