package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.dto.typesBrandsModels.TypeRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Types/Brands/Models", description = "Provide a fully mapped hierarchy of types, brands, and models")
public interface TypesBrandsModelsController {

    @Operation(
            summary = "List mapped types, brands, and models",
            description = "Returns the hierarchical catalog used by the device intake form to prevent invalid combinations.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Catalog hierarchy retrieved successfully")
    @GetMapping
    List<TypeRecord> listMappedTypesBrandsModels();
}
