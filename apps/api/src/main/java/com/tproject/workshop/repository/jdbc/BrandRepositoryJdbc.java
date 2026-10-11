package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.brand.BrandResponseDto;
import com.tproject.workshop.dto.brand.BrandSearchParam;
import org.springframework.data.domain.Page;

public interface BrandRepositoryJdbc {
    Page<BrandResponseDto> searchBrands(BrandSearchParam params);
}
