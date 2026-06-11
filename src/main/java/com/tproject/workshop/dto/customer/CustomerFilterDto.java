package com.tproject.workshop.dto.customer;

import com.tproject.workshop.utils.filter_utils.Ordenation;
import com.tproject.workshop.utils.filter_utils.OrderByDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record CustomerFilterDto(
    @Schema(description = "Customer ID filter", example = "1")
    Integer id,

    @Schema(description = "Exact CPF filter", example = "52998224725")
    String cpf,

    @Schema(description = "Phone number partial match filter", example = "11999887766")
    String phone,

    @Schema(description = "Fuzzy search query on customer name", example = "Joo")
    String searchName,

    @Schema(description = "Fuzzy search query on customer email", example = "gmal")
    String searchEmail,

    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "O número da página deve ser maior ou igual a zero")
    Integer page,

    @Schema(description = "Page size", example = "15")
    @Min(value = 1, message = "O tamanho da página deve ser maior que zero")
    Integer size,

    @Schema(description = "Ordering configuration (field and direction)")
    Ordenation ordenation
) {
    public CustomerFilterDto {
        if (page == null) page = 0;
        if (size == null) size = 15;
        if (ordenation == null) ordenation = new Ordenation("name", OrderByDirection.ASC);
        if (searchName != null && searchName.isBlank()) searchName = null;
        if (searchEmail != null && searchEmail.isBlank()) searchEmail = null;
    }
}
