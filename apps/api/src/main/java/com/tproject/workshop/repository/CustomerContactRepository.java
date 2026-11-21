package com.tproject.workshop.repository;

import com.tproject.workshop.model.CustomerContact;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Integer> {

    Optional<CustomerContact> findFirstByDeviceIdOrderByLastContactDescIdDesc(int deviceId);
}
