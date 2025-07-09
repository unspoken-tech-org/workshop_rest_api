package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.CustomerController;
import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import com.tproject.workshop.dto.customer.InputCustomerDtoRecord;
import com.tproject.workshop.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/customer")
public class CustomerControllerImpl implements CustomerController {

    private final CustomerService customerService;

    @Override
    public List<CustomerListOutputDto> search(CustomerFilterDto filters) {
        CustomerFilterDto nonNullFilters = Objects.requireNonNullElseGet(filters, CustomerFilterDto::new);
        return customerService.searchCustomers(nonNullFilters);
    }

    @Override
    public CustomerOutputDto findById(int id) {
        return customerService.findById(id);
    }

    @Override
    public CustomerOutputDto create(@Valid InputCustomerDtoRecord inputCustomerDto) {
        return customerService.saveCustomer(inputCustomerDto);
    }

    @Override
    public CustomerOutputDto update(int id, InputCustomerDtoRecord inputCustomerDto) {
        return customerService.updateCustomer(id, inputCustomerDto);
    }
}
