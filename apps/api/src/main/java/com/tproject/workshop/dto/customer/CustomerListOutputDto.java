package com.tproject.workshop.dto.customer;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CustomerListOutputDto {
    private int id;
    private String name;
    private String cpf;
    private String email;
    private String gender;
    private LocalDateTime createdAt;
    private String mainPhone;
}
