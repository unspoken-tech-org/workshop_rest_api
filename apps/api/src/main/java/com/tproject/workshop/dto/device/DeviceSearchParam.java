package com.tproject.workshop.dto.device;

import com.tproject.workshop.utils.filter_utils.Ordenation;
import com.tproject.workshop.utils.filter_utils.OrderByDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeviceSearchParam(
    @Schema(description = "Full-text search query across type, brand, model, and customer name", example = "samsung")
    @Size(max = 200, message = "A query de busca deve ter no máximo 200 caracteres")
    String searchQuery,

    @Schema(description = "Device ID filter", example = "1")
    Integer deviceId,

    @Schema(description = "Customer phone number filter", example = "11999887766")
    String customerPhone,

    @Schema(description = "Customer CPF filter", example = "52998224725")
    String customerCpf,

    @Schema(description = "List of device status filters", example = "[\"NOVO\",\"EM_ANDAMENTO\"]")
    @Size(max = 8, message = "No máximo 8 status por consulta")
    List<String> status,

    @Schema(description = "Filter by urgency flag", example = "true")
    Boolean urgency,

    @Schema(description = "Filter by revision flag", example = "false")
    Boolean revision,

    @Schema(description = "Start date for entry date range (yyyy-MM-dd)", example = "2026-01-01")
    String initialEntryDate,

    @Schema(description = "End date for entry date range (yyyy-MM-dd)", example = "2026-06-10")
    String finalEntryDate,

    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "O número da página deve ser maior ou igual a zero")
    Integer page,

    @Schema(description = "Page size", example = "15")
    @Min(value = 1, message = "O tamanho da página deve ser maior que zero")
    @Max(value = 100, message = "O tamanho da página deve ser no máximo 100")
    Integer size,

    @Schema(description = "Ordering configuration (field and direction)")
    Ordenation ordenation
) {
    public DeviceSearchParam {
        if (status == null) status = List.of();
        if (page == null) page = 0;
        if (size == null) size = 15;
        if (ordenation == null) ordenation = new Ordenation("entry_date", OrderByDirection.DESC);
        if (searchQuery != null && searchQuery.isBlank()) searchQuery = null;
    }
}
