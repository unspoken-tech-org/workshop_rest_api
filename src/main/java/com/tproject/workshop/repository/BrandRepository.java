package com.tproject.workshop.repository;

import com.tproject.workshop.model.Brand;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {

    List<Brand> findByBrandIgnoreCase(String name);
}
