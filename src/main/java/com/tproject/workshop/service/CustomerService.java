package com.tproject.workshop.service;

import com.tproject.workshop.dto.customer.InputCustomerDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    public List<Customer> findAllCustomers(){
        return customerRepository.findAll();
    }

    public Customer findById(int id){
        //TODO: implements error handling for not found entity
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Não existe cliente com id " + id));
    }

    public Customer addCustomer(InputCustomerDto inputCustomerDto){
        return customerRepository.save(inputCustomerDto.toCustomerModel());

    }
}
