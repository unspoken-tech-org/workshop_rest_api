package com.tproject.workshop.repository;

import com.tproject.workshop.model.BrandsModels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandsModelsRepository extends JpaRepository<BrandsModels, Integer> {

    @Query(value = "SELECT id, id_brand, id_model FROM brands_models WHERE id_brand = :idBrand AND id_model = :idModel"
            , nativeQuery = true)
    BrandsModels findByIdBrandAndIdModel(@Param("idBrand") int idBrand, @Param("idModel") int idModel);
}
