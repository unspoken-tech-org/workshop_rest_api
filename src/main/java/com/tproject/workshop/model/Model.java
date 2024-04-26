package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity(name = "models")
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int idModel;

    @Column(name = "model", nullable = false)
    private String model;

    @OneToMany(mappedBy = "idModel", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<BrandsModels> brandsModels;
}
