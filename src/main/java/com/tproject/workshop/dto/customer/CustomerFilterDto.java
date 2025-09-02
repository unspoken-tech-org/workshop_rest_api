package com.tproject.workshop.dto.customer;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CustomerFilterDto {
    private Integer id;
    private String name;
    private String cpf;
    private String phone;

    @Min(value = 0, message = "O número da página deve ser maior ou igual a zero")
    int page = 0;    
    
    @Min(value = 1, message = "O tamanho da página deve ser maior que zero")
    int size = 15;
} 