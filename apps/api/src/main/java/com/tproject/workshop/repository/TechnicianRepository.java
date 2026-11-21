package com.tproject.workshop.repository;

import com.tproject.workshop.model.Technician;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Integer> {
    Optional<Technician> findByNameIgnoreCase(String name);
}
