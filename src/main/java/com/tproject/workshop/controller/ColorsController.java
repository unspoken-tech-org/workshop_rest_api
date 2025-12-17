package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.model.Color;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Colors", description = "Expose the available color palette for device customization")
public interface ColorsController {

    @Operation(
            summary = "List colors",
            description = "Retrieves the list of colors configured for device registration and UI selection.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Colors retrieved successfully")
    @GetMapping
    List<Color> getColors();
}
