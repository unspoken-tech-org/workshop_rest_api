package com.tproject.workshop.repository;

import com.tproject.workshop.model.Model;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer> {

    Optional<Model> findByModelIgnoreCase(String name);

}
