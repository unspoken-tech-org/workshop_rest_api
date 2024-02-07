package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.CustomerController;
import com.tproject.workshop.dto.customer.InputCustomerDto;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("Customer")
public class CustomerControllerImpl implements CustomerController {

    private final CustomerService customerService;

    @Override
    public List<Customer> list() {
        return customerService.findAllCustomers();
    }

    @Override
    public Customer findById(int id) {
        return customerService.findById(id);
    }

    @Override
    public List<Customer> findByName(String name) {
        return customerService.findByName(name);
    }

    @Override
    public Customer create(InputCustomerDto inputCustomerDto) {
        return customerService.addCustomer(inputCustomerDto);
    }
}
