package com.tproject.workshop.repository;

import com.tproject.workshop.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelRepository extends JpaRepository<Model, Integer> {

    Model findByModelIgnoreCase(String name);

}
