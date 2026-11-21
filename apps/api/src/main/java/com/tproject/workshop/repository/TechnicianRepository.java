package com.tproject.workshop.repository;

import com.tproject.workshop.model.Technician;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Integer> {
    Optional<Technician> findByNameIgnoreCase(String name);

    @Modifying
    @Query(
            value =
                    "INSERT INTO technicians (technician_name, number) VALUES (:name, :number) "
                            + "ON CONFLICT (LOWER(TRIM(technician_name))) DO NOTHING",
            nativeQuery = true)
    int insertIgnore(@Param("name") String name, @Param("number") String number);
}
