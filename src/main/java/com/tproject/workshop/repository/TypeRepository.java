package com.tproject.workshop.repository;

import com.tproject.workshop.model.Type;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeRepository extends JpaRepository<Type, Integer> {

  List<Type> findByTypeContainingIgnoreCase(String type);
}
