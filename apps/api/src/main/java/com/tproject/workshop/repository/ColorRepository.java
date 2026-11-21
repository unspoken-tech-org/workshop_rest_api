package com.tproject.workshop.repository;

import com.tproject.workshop.model.Color;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ColorRepository extends JpaRepository<Color, Integer> {

    Optional<Color> findByColorIgnoreCase(String color);

    @Modifying
    @Query(
            value =
                    "INSERT INTO colors (color) VALUES (:color) ON CONFLICT (LOWER(TRIM(color))) DO NOTHING",
            nativeQuery = true)
    int insertIgnore(@Param("color") String color);
}
