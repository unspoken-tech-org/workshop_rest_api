package com.tproject.workshop.dto.typesBrandsModels;

import java.util.List;

public record BrandRecord(
        Integer idBrand,
        String brand,
        List<ModelRecord> models
) {
}
