package com.tproject.workshop.repository;

import com.tproject.workshop.model.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {

    Optional<Color> findByColorIgnoreCase(String color);

}
