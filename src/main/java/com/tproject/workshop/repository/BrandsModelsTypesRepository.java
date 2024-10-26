package com.tproject.workshop.repository;

import com.tproject.workshop.model.BrandsModelsTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandsModelsTypesRepository extends JpaRepository<BrandsModelsTypes, Integer> {

    @Query(value = "SELECT id, id_brand, id_model, id_type FROM brands_models_types WHERE id_brand = :idBrand AND id_model = :idModel  AND id_type = :idType"
            , nativeQuery = true)
    BrandsModelsTypes findByIdBrandAndIdModelAndIdType(@Param("idBrand") int idBrand, @Param("idModel") int idModel, @Param("idType") int idType);
}
