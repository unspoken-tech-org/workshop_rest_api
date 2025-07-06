package com.tproject.workshop.controller;

import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface TypesBrandsModelsController {

    @GetMapping
    List<TypeRecord> listMappedTypesBrandsModels();
}
