package com.tproject.workshop.service;

import com.tproject.workshop.dto.brand.BrandResponseDto;
import com.tproject.workshop.dto.brand.BrandSearchParam;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.exception.WorkshopException;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.repository.BrandRepository;
import com.tproject.workshop.utils.CatalogName;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class BrandService {

    private final BrandRepository brandRepository;
    private final ObservabilityEventRecorder eventRecorder;

    public List<BrandResponseDto> findAllDto(String name) {
        if (name == null || name.trim().isEmpty()) {
            return brandRepository.findAll().stream().map(this::toDto).toList();
        }
        return brandRepository.findByBrandContainingIgnoreCase(name).stream()
                .map(this::toDto)
                .toList();
    }

    public Brand findById(int id) {
        return brandRepository
                .findById(id)
                .orElseThrow(() -> NotFoundException.ofFeminine("Marca", id));
    }

    @Transactional
    public Brand createOrReturnExistentBrand(String brand) {
        return CatalogName.findOrCreate(
                brand,
                brandRepository::findByBrandIgnoreCase,
                brandRepository::insertIgnore,
                () ->
                        eventRecorder.record(
                                ObservabilityEventCatalog.CATALOG_ENTRY_CREATED,
                                Map.of("catalog.kind", "brand")),
                () -> new WorkshopException("Erro ao criar ou buscar marca: " + brand));
    }

    private BrandResponseDto toDto(Brand brand) {
        return new BrandResponseDto(brand.getIdBrand(), brand.getBrand());
    }

    public Page<BrandResponseDto> searchBrands(BrandSearchParam params) {
        return brandRepository.searchBrands(params);
    }

    @Transactional
    public BrandResponseDto createBrand(String brandName) {
        Brand brand = createOrReturnExistentBrand(brandName);
        return new BrandResponseDto(brand.getIdBrand(), brand.getBrand());
    }
}
