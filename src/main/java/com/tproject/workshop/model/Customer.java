package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.validator.constraints.br.CPF;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idCustomer;
    @Column(name = "name", nullable = false)
    String name;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "insert_date", nullable = false)
    Timestamp insertDate;
    @CPF
    @Column(name = "cpf", nullable = false)
    String cpf;
    @Column(name = "gender", nullable = false)
    String gender;
    @Column(name = "email")
    String email;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<CustomerPhone> customerPhones;
}
