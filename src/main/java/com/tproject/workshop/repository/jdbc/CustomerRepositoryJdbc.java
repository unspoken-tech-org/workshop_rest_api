package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.customer.CustomerOutputDto;

import java.util.Optional;

public interface CustomerRepositoryJdbc {

    Optional<CustomerOutputDto> findCustomerById(int id);
}
