package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.sql.Timestamp;

@Data
@Entity(name = "clientes")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int id;
    @Column(name = "nome", nullable = false)
    String name;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "datacadastro", nullable = false)
    Timestamp insertDate;
    @Column(name = "cpf", nullable = false)
    String  cpf;
    @Column(name = "sexo", nullable = false)
    char gender;
    @Column(name = "email")
    String mail;
    @Column(name = "telefone", nullable = false)
    String phone;
    @Column(name = "whatsapp")
    String cellPhone;
}
