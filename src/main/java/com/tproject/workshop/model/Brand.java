package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int idBrand;

    @Column(name = "brand", nullable = false)
    private String brand;

    @OneToMany(mappedBy = "idBrand", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<BrandsModels> brandsModels;
}
