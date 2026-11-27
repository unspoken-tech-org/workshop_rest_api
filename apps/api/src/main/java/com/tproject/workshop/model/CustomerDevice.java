package com.tproject.workshop.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Physical device owned by a customer ({@code AP-XXXX}). Each {@link ServiceOrder} is one visit of
 * this device to the workshop. Until the contraction migration the API also copies customer,
 * type/brand/model and colors to the legacy columns of {@code devices}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "customer_devices")
public class CustomerDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_customer", nullable = false)
    @ToString.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_brand_model_type", nullable = false)
    @ToString.Exclude
    private BrandsModelsTypes brandsModelsTypes;

    @Column(name = "color_ids", nullable = false)
    private List<Integer> colorIds;

    @Column(name = "serial_number", length = 120)
    private String serialNumber;

    @Column(name = "label", length = 120)
    private String label;

    @Column(name = "physical_notes")
    private String physicalNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public CustomerDevice(
            Customer customer, BrandsModelsTypes brandsModelsTypes, List<Integer> colorIds) {
        this.customer = customer;
        this.brandsModelsTypes = brandsModelsTypes;
        this.colorIds = colorIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CustomerDevice that = (CustomerDevice) o;
        return getId() != 0 && getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
