package com.tproject.workshop.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CustomerPhoneId implements Serializable {
    
    @Column(name = "id_customer")
    private int customerId;
    
    @Column(name = "id_phone")
    private int phoneId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerPhoneId that = (CustomerPhoneId) o;
        return customerId == that.customerId && phoneId == that.phoneId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(customerId, phoneId);
    }
}
