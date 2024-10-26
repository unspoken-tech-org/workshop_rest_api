package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "brands_models_types")
public class BrandsModelsTypes {
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

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type")
    private Type idType;
}
