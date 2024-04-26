package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.BrandsModels;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.repository.BrandsModelsRepository;
import com.tproject.workshop.repository.BrandsModelsTypesRepository;
import com.tproject.workshop.repository.TypeRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandsModelsAndTypesService {

    private final BrandsModelsRepository brandsModelsRepository;
    private final BrandsModelsTypesRepository brandsModelsTypesRepository;
    private final TypeRepository typeRepository;
    private final BrandService brandService;
    private final ModelService modelService;

    private BrandsModels findOrCreateBrandModel(@NotNull int brandId, @NotNull int modelId) {
        var searchedBrandModel =  brandsModelsRepository.findByIdBrandAndIdModel(brandId, modelId);
        if (searchedBrandModel == null) {
            var newBrandModel = new BrandsModels();
            newBrandModel.setIdBrand(brandService.findById(brandId));
            newBrandModel.setIdModel(modelService.findById(modelId));
            return brandsModelsRepository.save(newBrandModel);
        }
        return searchedBrandModel;
    }

    public BrandsModelsTypes findOrCreateBrandModelType(@NotNull int brandId, @NotNull int modelId, @NotNull int typeId) {
        var brandModel = findOrCreateBrandModel(brandId, modelId);
        brandsModelsRepository.flush();

        var searchedBrandModelType = brandsModelsTypesRepository.findByIdBrandModelAndIdType(brandModel.getId(), typeId);
        if (searchedBrandModelType == null) {
            var newBrandModelType = new BrandsModelsTypes();
            newBrandModelType.setIdBrandModel(brandsModelsRepository.findById(brandModel.getId())
                    .orElseThrow(() -> new NotFoundException(String.format("Marca/Modelo com id %d não encontrado", brandModel.getId()))));

            newBrandModelType.setIdType(typeRepository.findById(typeId)
                    .orElseThrow(() -> new NotFoundException(String.format("Tipo com id %d não encontrado", typeId))));

            return brandsModelsTypesRepository.save(newBrandModelType);
        }
        return searchedBrandModelType;
    }
}
