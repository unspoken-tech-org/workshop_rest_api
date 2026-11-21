package com.tproject.workshop.repository;

import com.tproject.workshop.model.Customer;
import com.tproject.workshop.repository.jdbc.CustomerRepositoryJdbc;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository
        extends JpaRepository<Customer, Integer>, CustomerRepositoryJdbc {
    List<Customer> findByNameContaining(String name);

    Optional<Customer> findFirstByCpf(String cpf);
}
