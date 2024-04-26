package com.tproject.workshop.repository;

import com.tproject.workshop.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {

    Brand findByBrandIgnoreCase(String name);
}
