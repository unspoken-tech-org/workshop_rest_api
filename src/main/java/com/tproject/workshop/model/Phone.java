package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity(name = "phones")
public class Phone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idCellphone;
    @Column(name = "number")
    String number;
    @Column(name = "name")
    String name;
    @Column(name = "is_main")
    boolean main;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "id_customer")
    Customer customer;
}
