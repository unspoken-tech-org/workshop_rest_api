package com.tproject.workshop.repository;

import com.tproject.workshop.model.Brand;
import com.tproject.workshop.repository.jdbc.BrandRepositoryJdbc;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer>, BrandRepositoryJdbc {

    Optional<Brand> findByBrandIgnoreCase(String brand);

    List<Brand> findByBrandContainingIgnoreCase(String brand);

    Optional<Brand> findByNameKey(String nameKey);

    @Modifying
    @Query(
            value = "INSERT INTO brands (brand) VALUES (:brand) ON CONFLICT (name_key) DO NOTHING",
            nativeQuery = true)
    int insertIgnore(@Param("brand") String brand);
}
