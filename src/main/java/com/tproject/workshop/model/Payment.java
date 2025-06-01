package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
    @Column(name = "payment_date", updatable = false)
    private Timestamp paymentDate;

    @Column(name = "payment_type", updatable = false)
    private String paymentType;

    @Column(name = "payment_value", updatable = false)
    private BigDecimal paymentValue;

    @Column(name = "category", updatable = false)
    private String category;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_device")
    private Device device;
}
