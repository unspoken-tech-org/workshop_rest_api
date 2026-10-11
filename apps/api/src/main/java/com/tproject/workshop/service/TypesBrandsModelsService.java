package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.TypesBrandsModelsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypesBrandsModelsService {

    private final TypesBrandsModelsRepository typesBrandsModelsRepository;
    private final TypeService typeService;
    private final BrandService brandService;
    private final ModelService modelService;

    @Transactional
    public BrandsModelsTypes createOrReturnExistentBrandModelType(TypeBrandModelInputDtoRecord typeBrandModel) {
        var typeInput = typeBrandModel.type();
        var brandInput = typeBrandModel.brand();
        var modelInput = typeBrandModel.model();

        Type type = typeService.createOrReturnExistentType(typeInput);
        Brand brand = brandService.createOrReturnExistentBrand(brandInput);
        Model model = modelService.createOrReturnExistentModel(modelInput);

        var entity = typesBrandsModelsRepository.findByIdBrandAndIdModelAndIdType(type.getIdType(), brand.getIdBrand(),
                model.getIdModel());

        if (entity.isPresent()) {
            return entity.get();
        }

        BrandsModelsTypes brandModelType = new BrandsModelsTypes();
        brandModelType.setIdBrand(brand);
        brandModelType.setIdModel(model);
        brandModelType.setIdType(type);

        try {
            return typesBrandsModelsRepository.saveAndFlush(brandModelType);
        } catch (DataIntegrityViolationException e) {
            return typesBrandsModelsRepository.findByIdBrandAndIdModelAndIdType(type.getIdType(), brand.getIdBrand(),
                            model.getIdModel())
                    .orElseThrow(() -> new RuntimeException("Erro ao criar ou buscar associação de tipo, marca e modelo", e));
        }
    }

    @Transactional(readOnly = true)
    public List<TypeRecord> getTypesBrandsModels() {
        return typesBrandsModelsRepository.getTypesBrandsModels();
    }

}
