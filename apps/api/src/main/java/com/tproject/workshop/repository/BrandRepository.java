package com.tproject.workshop.repository;

import com.tproject.workshop.model.Brand;
import java.util.List;
import java.util.Optional;

import com.tproject.workshop.repository.jdbc.BrandRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer>, BrandRepositoryJdbc {

    Optional<Brand> findByBrandIgnoreCase(String brand);
    List<Brand> findByBrandContainingIgnoreCase(String brand);

    @Modifying
    @Query(value = "INSERT INTO brands (brand) VALUES (:brand) ON CONFLICT (LOWER(TRIM(brand))) DO NOTHING", nativeQuery = true)
    int insertIgnore(@Param("brand") String brand);
}
