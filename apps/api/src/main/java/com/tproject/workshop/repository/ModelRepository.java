package com.tproject.workshop.repository;

import com.tproject.workshop.model.Model;

import java.util.Optional;

import com.tproject.workshop.repository.jdbc.ModelRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer>, ModelRepositoryJdbc {

    Optional<Model> findByModelIgnoreCase(String name);

}
