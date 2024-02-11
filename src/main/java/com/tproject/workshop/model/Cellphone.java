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
    int number;
    @Column(name = "whats")
    int whatsapp;
    @Column(name = "tipo")
    char type;
    @JoinColumn(name = "idCliente", referencedColumnName = "idCustomer")
    int idCustomer;
}
