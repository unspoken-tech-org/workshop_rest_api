package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity(name = "brands_models_types")
public class BrandsModelsTypes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_brand")
    @ToString.Exclude
    private Brand idBrand;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_model")
    @ToString.Exclude
    private Model idModel;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_type")
    @ToString.Exclude
    private Type idType;
}
