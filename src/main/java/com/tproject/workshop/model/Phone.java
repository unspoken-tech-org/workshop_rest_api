package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "phones")
public class Phone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idCellphone;
    @Column(name = "number")
    String number;
    @Column(name = "whats")
    boolean whatsapp;
    @Column(name = "type")
    String type;
    @Column(name = "is_main")
    boolean main;
    @ManyToOne()
    @JoinColumn(name = "id_customer")
    Customer customer;
}
