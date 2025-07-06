package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Entity(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Integer idBrand;

    @Column(name = "brand", nullable = false)
    private String brand;

    @JsonIgnore
    @OneToMany(mappedBy = "idBrand", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<BrandsModelsTypes> brandsModelsTypes;
}
