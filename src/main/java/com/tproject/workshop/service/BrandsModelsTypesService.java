package com.tproject.workshop.service;

import com.tproject.workshop.model.Brand;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.BrandsModelsTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class BrandsModelsTypesService {

    private final BrandsModelsTypesRepository brandsModelsTypesRepository;
    private final BrandService brandService;
    private final ModelService modelService;
    private final TypeService typeRepository;

    BrandsModelsTypes createBrandModelType(int brandId, int modelId, int typeId){
        BrandsModelsTypes entity = brandsModelsTypesRepository.findByIdBrandAndIdModelAndIdType(brandId, modelId, typeId);

        if(entity != null){
            return entity;
        }

        Brand brand =  brandService.findById(brandId);
        Model model = modelService.findById(modelId);
        Type type = typeRepository.findById(typeId);

        BrandsModelsTypes brandModelType = new BrandsModelsTypes();
        brandModelType.setIdBrand(brand);
        brandModelType.setIdModel(model);
        brandModelType.setIdType(type);

        return brandsModelsTypesRepository.save(brandModelType);
    }

}
