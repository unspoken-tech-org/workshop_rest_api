package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity(name = "types")
public class Type {
    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idType;
    @Column(name = "type", nullable = false)
    private String type;

    @OneToMany(mappedBy = "idType",cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<BrandsModelsTypes> brandsModelsTypes;
}
