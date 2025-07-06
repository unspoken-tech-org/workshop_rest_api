package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.BrandInputDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Brand save(BrandInputDto brandInput) {
        var brandFound = brandRepository.findByBrandIgnoreCase(brandInput.brand());
        if (brandFound.isPresent()) {
            return brandFound.get();
        }

        var newBrand = new Brand();
        var brandName = brandInput.brand().toLowerCase();
        newBrand.setBrand(brandName);

        return brandRepository.save(newBrand);

    }

    public List<Brand> findAll(String name) {
        if (name == null || name.trim().isEmpty()) {
            return brandRepository.findAll();
        }

        return brandRepository.findByBrandContainingIgnoreCase(name);
    }

}
