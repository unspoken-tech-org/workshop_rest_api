package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.customer.CustomerFilterDto;
import com.tproject.workshop.dto.customer.CustomerListOutputDto;
import com.tproject.workshop.dto.customer.CustomerOutputDto;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface CustomerRepositoryJdbc {

    Optional<CustomerOutputDto> findCustomerById(int id);

    Page<CustomerListOutputDto> searchCustomers(CustomerFilterDto filters);
}
