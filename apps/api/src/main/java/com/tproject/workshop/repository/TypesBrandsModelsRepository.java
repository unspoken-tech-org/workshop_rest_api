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

    @Query("SELECT b FROM brands_models_types b WHERE b.idBrand.idBrand = :idBrand AND b.idModel.idModel = :idModel AND b.idType.idType = :idType")
    Optional<BrandsModelsTypes> findByIdBrandAndIdModelAndIdType(@Param("idType") int idType,
                                                                 @Param("idBrand") int idBrand, @Param("idModel") int idModel);
}
