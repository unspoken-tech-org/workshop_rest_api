package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "telefones")
public class Cellphone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idCellphone;
    @Column(name = "numero")
    String number;
    @Column(name = "whats")
    boolean whatsapp;
    @Column(name = "tipo")
    String type;
    @ManyToOne()
    @JoinColumn(name = "idcliente")
    Customer customer;
}
