package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "phones")
public class Phone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idCellphone;
    @Column(name = "number")
    String number;
    @Column(name = "alias")
    String phoneAlias;
    
    @OneToMany(mappedBy = "phone", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CustomerPhone> customerPhones;
}
