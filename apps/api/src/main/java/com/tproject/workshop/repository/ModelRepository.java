package com.tproject.workshop.repository;

import com.tproject.workshop.model.Model;
import com.tproject.workshop.repository.jdbc.ModelRepositoryJdbc;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer>, ModelRepositoryJdbc {

    Optional<Model> findByModelIgnoreCase(String name);

    Optional<Model> findByNameKey(String nameKey);

    @Modifying
    @Query(
            value = "INSERT INTO models (model) VALUES (:model) ON CONFLICT (name_key) DO NOTHING",
            nativeQuery = true)
    int insertIgnore(@Param("model") String model);
}
