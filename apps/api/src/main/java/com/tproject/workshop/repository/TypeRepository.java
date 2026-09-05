package com.tproject.workshop.repository;

import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.jdbc.TypeRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TypeRepository extends JpaRepository<Type, Integer>, TypeRepositoryJdbc {

  List<Type> findByTypeContainingIgnoreCase(String type);
  Optional<Type> findByTypeIgnoreCase(String type);
}
