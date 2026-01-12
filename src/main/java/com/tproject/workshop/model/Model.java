package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@Entity(name = "models")
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Integer idModel;

    @Column(name = "model", nullable = false)
    private String model;

    @OneToMany(mappedBy = "idModel", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<BrandsModelsTypes> brandsModelsTypes;
}
