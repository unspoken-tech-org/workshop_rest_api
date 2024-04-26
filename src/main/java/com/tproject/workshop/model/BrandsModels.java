package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity(name = "brands_models")
public class BrandsModels {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_brand")
    private Brand idBrand;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_model")
    private Model idModel;

    @OneToMany(mappedBy = "idBrandModel", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<BrandsModelsTypes> brandsModelsTypes;
}
