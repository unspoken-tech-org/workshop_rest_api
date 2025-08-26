package com.tproject.workshop.dto.customer;

import lombok.Data;

@Data
public class CustomerFilterDto {
    private Integer id;
    private String name;
    private String cpf;
    private String phone;

    int page = 0;    
    int size = 15;
} 