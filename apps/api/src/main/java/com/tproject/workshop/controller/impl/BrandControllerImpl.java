package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.BrandController;
import com.tproject.workshop.dto.PageResponse;
import com.tproject.workshop.dto.brand.BrandResponseDto;
import com.tproject.workshop.dto.brand.BrandSearchParam;
import com.tproject.workshop.dto.brand.CreateBrandRequest;
import com.tproject.workshop.service.BrandService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/brand")
public class BrandControllerImpl implements BrandController {

    private final BrandService brandService;

    @Override
    public List<BrandResponseDto> list(String name) {
        return brandService.findAllDto(name);
    }

    @Override
    public PageResponse<BrandResponseDto> search(@Valid BrandSearchParam params) {
        return PageResponse.from(brandService.searchBrands(params));
    }

    @Override
    public BrandResponseDto create(CreateBrandRequest request) {
        return brandService.createBrand(request.brand());
    }
}
