package com.tproject.workshop.repository;

import com.tproject.workshop.model.Phone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneRepository extends JpaRepository<Phone, Integer> {

    Optional<Phone> findByNumber(String number);

    @Modifying
    @Query(value = "INSERT INTO phones (number) VALUES (:number) ON CONFLICT (number) DO NOTHING", nativeQuery = true)
    int insertIgnore(@Param("number") String number);

}
