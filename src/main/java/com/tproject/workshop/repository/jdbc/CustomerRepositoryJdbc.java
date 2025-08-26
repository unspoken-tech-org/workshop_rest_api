package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;

import java.util.Optional;

import org.springframework.data.domain.Page;

public interface CustomerRepositoryJdbc {

    Optional<CustomerOutputDto> findCustomerById(int id);

    Page<CustomerListOutputDto> findCustomersByFilter(CustomerFilterDto filters);
}
