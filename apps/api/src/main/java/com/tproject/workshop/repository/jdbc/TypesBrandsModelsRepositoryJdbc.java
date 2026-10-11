package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;

import java.util.List;

public interface TypesBrandsModelsRepositoryJdbc {
    List<TypeRecord> getTypesBrandsModels();
}
