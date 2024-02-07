package com.tproject.workshop.repository;

import com.tproject.workshop.model.Cellphone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CellphoneRepository extends JpaRepository<Cellphone, Integer> {
}
