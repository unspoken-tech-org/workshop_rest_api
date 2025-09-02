package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "customer_phones")
public class CustomerPhone {
    
    @EmbeddedId
    private CustomerPhoneId id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("customerId")
    @JoinColumn(name = "id_customer")
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("phoneId")
    @JoinColumn(name = "id_phone")
    private Phone phone;
    
    @Column(name = "is_main")
    private boolean isMain;
    
    // Convenience constructor
    public CustomerPhone(Customer customer, Phone phone, boolean isMain) {
        this.customer = customer;
        this.phone = phone;
        this.isMain = isMain;
        this.id = new CustomerPhoneId(customer.getIdCustomer(), phone.getIdCellphone());
    }
}
