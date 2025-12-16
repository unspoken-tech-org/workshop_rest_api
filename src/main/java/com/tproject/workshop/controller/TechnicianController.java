package com.tproject.workshop.controller;

import com.tproject.workshop.config.openapi.ApiGlobalResponses;
import com.tproject.workshop.model.Technician;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Technicians", description = "Access the roster of technicians responsible for device servicing")
public interface TechnicianController {

    @Operation(
            summary = "Find technician by id",
            description = "Retrieves a technician to attribute devices, follow tasks, or display ownership in dashboards.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Technician retrieved successfully")
    @GetMapping("/{id}")
    Technician find(@Parameter(description = "Technician identifier") @PathVariable("id") Integer id);

    @Operation(
            summary = "List technicians",
            description = "Returns every active technician, supporting assignment dropdowns and routing rules.")
    @ApiGlobalResponses
    @ApiResponse(responseCode = "200", description = "Technicians listed successfully")
    @GetMapping("/list")
    List<Technician> list();

}
