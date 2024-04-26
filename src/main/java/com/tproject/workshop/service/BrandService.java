package com.tproject.workshop.service;

import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Brand;
import com.tproject.workshop.repository.BrandRepository;
import com.tproject.workshop.utils.UtilsString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public List<Brand> findAll(){
        return brandRepository.findAll();
    }

    public Brand findById(int id){
        return brandRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Marca com id %d não encontrada", id)));
    }

    public Brand save(Brand brand){
        var brandFound = findByName(brand.getBrand());
        if(brandFound == null){
            var brandName = UtilsString.capitalizeEachWord(brand.getBrand());
            brand.setBrand(brandName);

            return brandRepository.save(brand);
        }
        return brand;
    }

    private Brand findByName(String name){
        return brandRepository.findByBrandIgnoreCase(name);
    }

}
