package com.tproject.workshop.dto.customer;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.util.List;

public record InputCustomerDtoRecord(
        @NotBlank
        String name,
        @NotBlank
        @CPF(message = "Número de CPF inválido")
        String cpf,
        @NotBlank
        String gender,
        String email,
        @NotEmpty
        @Size(min = 1, max = 4, message = "Não é possível ter menos de 1 ou mais de 4 telefones")
        @Valid
        List<InputPhoneDto> phones
) {
}
