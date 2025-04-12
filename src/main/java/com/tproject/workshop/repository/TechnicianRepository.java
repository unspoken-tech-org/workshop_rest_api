package com.tproject.workshop.repository;

import com.tproject.workshop.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Integer> {
    Technician findByNameIgnoreCase(String name);
}
