package com.tproject.workshop.repository;

import com.tproject.workshop.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, Integer> {

    Optional<Phone> findByNumber(String number);

}
