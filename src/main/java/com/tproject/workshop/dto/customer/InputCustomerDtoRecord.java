package com.tproject.workshop.dto.customer;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.util.List;

public record InputCustomerDtoRecord(
        @Schema(description = "Full customer name", example = "John Doe")
        @NotBlank
        String name,
        @Schema(description = "Brazilian CPF used as a unique identifier", example = "75386544020")
        @NotBlank
        @CPF(message = "Número de CPF inválido")
        String cpf,
        @Schema(description = "Self-declared gender label", example = "male")
        @NotBlank
        String gender,
        @Schema(description = "Primary email for notifications", example = "john.doe@example.com")
        String email,
        @Schema(
                description = "Customer phone numbers including flags for the primary contact",
                example = "[{\"id\":1,\"name\":\"Mobile\",\"number\":\"11999991111\",\"isPrimary\":true}]")
        @NotEmpty
        @Size(min = 1, max = 4, message = "Não é possível ter menos de 1 ou mais de 4 telefones")
        @Valid
        List<InputPhoneDto> phones
) {
}
