package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.TypeBrandModelInputDtoRecord;
import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;
import com.tproject.workshop.exception.WorkshopException;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Model;
import com.tproject.workshop.model.Type;
import com.tproject.workshop.repository.TypesBrandsModelsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TypesBrandsModelsService {

    private final TypesBrandsModelsRepository typesBrandsModelsRepository;
    private final TypeService typeService;
    private final BrandService brandService;
    private final ModelService modelService;

    @Transactional
    public BrandsModelsTypes createOrReturnExistentBrandModelType(
            TypeBrandModelInputDtoRecord typeBrandModel) {
        var typeInput = typeBrandModel.type();
        var brandInput = typeBrandModel.brand();
        var modelInput = typeBrandModel.model();

        Type type = typeService.createOrReturnExistentType(typeInput);
        Brand brand = brandService.createOrReturnExistentBrand(brandInput);
        Model model = modelService.createOrReturnExistentModel(modelInput);

        return typesBrandsModelsRepository
                .findByIdBrandAndIdModelAndIdType(
                        type.getIdType(), brand.getIdBrand(), model.getIdModel())
                .orElseGet(() -> {
                    typesBrandsModelsRepository.insertIgnore(
                            brand.getIdBrand(), model.getIdModel(), type.getIdType());
                    return typesBrandsModelsRepository
                            .findByIdBrandAndIdModelAndIdType(
                                    type.getIdType(), brand.getIdBrand(), model.getIdModel())
                            .orElseThrow(() ->
                                    new WorkshopException(
                                            "Erro ao criar ou buscar associação de tipo, marca e modelo"));
                });
    }

    public List<TypeRecord> getTypesBrandsModels() {
        return typesBrandsModelsRepository.getTypesBrandsModels();
    }
}
