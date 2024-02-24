package com.tproject.workshop.repository;

import com.tproject.workshop.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CellphoneRepository extends JpaRepository<Phone, Integer> {
}
