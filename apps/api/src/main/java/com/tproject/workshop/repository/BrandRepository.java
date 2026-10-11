package com.tproject.workshop.repository;

import com.tproject.workshop.model.Brand;
import java.util.List;
import java.util.Optional;

import com.tproject.workshop.repository.jdbc.BrandRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer>, BrandRepositoryJdbc {

    Optional<Brand> findByBrandIgnoreCase(String brand);
    List<Brand> findByBrandContainingIgnoreCase(String brand);
}
