package com.tproject.workshop.repository;

import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.jdbc.TypeRepositoryJdbc;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeRepository extends JpaRepository<Type, Integer>, TypeRepositoryJdbc {

    List<Type> findByTypeContainingIgnoreCase(String type);

    Optional<Type> findByTypeIgnoreCase(String type);

    Optional<Type> findByNameKey(String nameKey);

    @Modifying
    @Query(
            value = "INSERT INTO types (type) VALUES (:type) ON CONFLICT (name_key) DO NOTHING",
            nativeQuery = true)
    int insertIgnore(@Param("type") String type);
}
