package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.TypesBrandsModelsController;
import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;
import com.tproject.workshop.service.TypesBrandsModelsService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/types-brands-models")
@AllArgsConstructor
public class TypesBrandsModelsControllerImpl implements TypesBrandsModelsController {
    final TypesBrandsModelsService typesBrandsModelsService;

    @Override
    public List<TypeRecord> listMappedTypesBrandsModels() {
        return typesBrandsModelsService.getTypesBrandsModels();
    }
}
