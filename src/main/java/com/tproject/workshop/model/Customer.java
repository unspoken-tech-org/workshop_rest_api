package com.tproject.workshop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.validator.constraints.br.CPF;

import java.sql.Timestamp;

@Data
@Entity(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idCustomer;
    @Column(name = "name", nullable = false)
    String name;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "insertDate", nullable = false)
    Timestamp insertDate;
    @CPF
    @Column(name = "cpf", nullable = false)
    String  cpf;
    @Column(name = "gender", nullable = false)
    char gender;
    @Column(name = "email")
    String mail;
    @Column(name = "phone", nullable = false)
    String phone;
    @Column(name = "whatsapp")
    String cellPhone;
}
