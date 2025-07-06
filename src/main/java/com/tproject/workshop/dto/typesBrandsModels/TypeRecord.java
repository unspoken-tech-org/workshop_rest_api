package com.tproject.workshop.dto.typesBrandsModels;

import java.util.List;

public record TypeRecord(
        Integer idType,
        String type,
        List<BrandRecord> brands
) {
}
