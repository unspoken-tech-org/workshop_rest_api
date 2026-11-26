package com.tproject.workshop.model;

import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.enums.PaymentMethodEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
@Entity(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @Column(name = "payment_date", updatable = false)
    private Timestamp paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", updatable = false)
    private PaymentMethodEnum paymentType;

    @Column(name = "payment_value", updatable = false)
    private BigDecimal paymentValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", updatable = false)
    private PaymentCategoryEnum category;

    @Column(name = "received_by", updatable = false)
    private String receivedBy;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_device")
    @ToString.Exclude
    private Device device;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Payment that = (Payment) o;
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
