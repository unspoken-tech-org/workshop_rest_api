package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> findAll() {
        return brandRepository.findAll();
    }

    public Brand findById(int id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Marca com id %d não encontrada", id)));
    }

    @Transactional
    public Brand createOrReturnExistentBrand(String brand) {
        var brandFound = brandRepository.findByBrandIgnoreCase(brand);
        if (brandFound.isPresent()) {
            return brandFound.get();
        }

        var brandName = brand.toLowerCase().trim().replaceAll("\\s+", " ");
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

    public List<Brand> findAll(String name) {
        if (name == null || name.trim().isEmpty()) {
            return brandRepository.findAll();
        }

        return brandRepository.findByBrandContainingIgnoreCase(name);
    }

}
