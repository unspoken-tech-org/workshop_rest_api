package com.tproject.workshop.repository;

import com.tproject.workshop.model.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Integer> {

    Optional<CustomerContact> findFirstByDeviceIdOrderByLastContactDescIdDesc(int deviceId);
}
