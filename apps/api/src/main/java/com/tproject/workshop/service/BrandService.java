package com.tproject.workshop.service;

import com.tproject.workshop.dto.brand.BrandResponseDto;
import com.tproject.workshop.dto.brand.BrandSearchParam;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.repository.BrandRepository;
import com.tproject.workshop.repository.jdbc.BrandRepositoryJdbc;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public List<BrandResponseDto> findAllDto(String name) {
        if (name == null || name.trim().isEmpty()) {
            return brandRepository.findAll().stream()
                    .map(this::toDto)
                    .toList();
        }
        return brandRepository.findByBrandContainingIgnoreCase(name).stream()
                .map(this::toDto)
                .toList();
    }


    public Brand findById(int id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Marca com id %d não encontrada", id)));
    }

    @Transactional
    public Brand createOrReturnExistentBrand(String brand) {
        var brandName = brand.toLowerCase().trim().replaceAll("\\s+", " ");
        var brandFound = brandRepository.findByBrandIgnoreCase(brandName);

        if (brandFound.isPresent()) {
            return brandFound.get();
        }

        try {
            var newBrand = new Brand();
            newBrand.setBrand(brandName);

            return brandRepository.save(newBrand);
        } catch (DataIntegrityViolationException e) {
            return findBrandOnCreate(brandName, brand);
        }
    }

    private Brand findBrandOnCreate(String brandName, String originalBrand) {
        return brandRepository.findByBrandIgnoreCase(brandName)
                .orElseThrow(() -> new NotFoundException("Erro ao criar ou buscar marca: " + originalBrand));
    }

    private BrandResponseDto toDto(Brand brand) {
        return new BrandResponseDto(
                brand.getIdBrand(),
                brand.getBrand()
        );
    }

    @Transactional(readOnly = true)
    public Page<BrandResponseDto> searchBrands(BrandSearchParam params) {
        return brandRepository.searchBrands(params);
    }

    @Transactional
    public BrandResponseDto createBrand(String brandName) {
        Brand brand = createOrReturnExistentBrand(brandName);
        return new BrandResponseDto(brand.getIdBrand(), brand.getBrand());
    }
}
