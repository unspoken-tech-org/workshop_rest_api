package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "Telefones")
public class Cellphone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTelefone", updatable = false)
    int idCellphone;
    @Column(name = "numero")
    int number;
    @Column(name = "Whatsapp")
    int whatsapp;
    @Column(name = "tipo")
    char type;
    @ManyToOne
    @JoinColumn(name = "idCustomer", referencedColumnName = "idCustomer")
    private Customer idCustomer;
}
