package com.tproject.workshop.model;

import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.enums.PaymentMethodEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
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
    private Device device;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
