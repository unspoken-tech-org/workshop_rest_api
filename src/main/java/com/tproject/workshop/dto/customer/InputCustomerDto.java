package com.tproject.workshop.dto.customer;

import com.tproject.workshop.dto.cellphone.InputPhoneDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

import java.util.List;

public record InputCustomerDto(
        @NotNull
        String name,
        @CPF(message = "Número de CPF inválido")
        String cpf,
        @NotNull
        String gender,
        String email,
        @NotEmpty
        List<InputPhoneDto> phones
) {
}
