package com.tproject.workshop.repository;

import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.repository.jdbc.TypesBrandsModelsRepositoryJdbc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypesBrandsModelsRepository
        extends JpaRepository<BrandsModelsTypes, Integer>, TypesBrandsModelsRepositoryJdbc {

    @Query(value = "SELECT id, id_brand, id_model, id_type FROM brands_models_types WHERE id_brand = :idBrand AND id_model = :idModel  AND id_type = :idType", nativeQuery = true)
    Optional<BrandsModelsTypes> findByIdBrandAndIdModelAndIdType(@Param("idType") int idType,
                                                                 @Param("idBrand") int idBrand, @Param("idModel") int idModel);
}
