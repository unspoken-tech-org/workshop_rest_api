package com.tproject.workshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "customer_phones")
public class CustomerPhone {

    @EmbeddedId private CustomerPhoneId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("customerId")
    @JoinColumn(name = "id_customer")
    @ToString.Exclude
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("phoneId")
    @JoinColumn(name = "id_phone")
    @ToString.Exclude
    private Phone phone;

    @Column(name = "is_main")
    private boolean isMain;

    @Column(name = "alias")
    private String phoneAlias;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Convenience constructor
    public CustomerPhone(Customer customer, Phone phone, boolean isMain) {
        this(customer, phone, isMain, null);
    }

    public CustomerPhone(Customer customer, Phone phone, boolean isMain, String phoneAlias) {
        this.customer = customer;
        this.phone = phone;
        this.isMain = isMain;
        this.phoneAlias = phoneAlias;
        this.id = new CustomerPhoneId(customer.getIdCustomer(), phone.getIdCellphone());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CustomerPhone that = (CustomerPhone) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
