package com.tproject.workshop.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
@ToString
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    Timestamp createdAt;

    @CPF
    @Column(name = "cpf", nullable = false)
    String cpf;

    @Column(name = "gender", nullable = false)
    String gender;

    @Column(name = "email")
    String email;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<CustomerPhone> customerPhones;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Customer that = (Customer) o;
        return getIdCustomer() != 0 && getIdCustomer() == that.getIdCustomer();
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
