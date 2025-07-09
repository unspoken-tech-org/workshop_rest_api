package com.tproject.workshop.dto.customer;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerListOutputDto {
    private int id;
    private String name;
    private String cpf;
    private String email;
    private String gender;
    private LocalDateTime insertDate;
    private String mainPhone;
} 