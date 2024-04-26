package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Entity(name = "sales")
public class Sale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idSell;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "sale_date", nullable = false)
    Timestamp insertDate;
    @Column(name = "invoice_type")
    String invoiceType;
    @Column(name = "invoice_number")
    String invoiceNumber;
    @Column(name = "total")
    BigDecimal total;
    @Column(name = "paid_total")
    BigDecimal paidTotal;
    @Column(name = "sell_type")
    String sellType;
    @ManyToOne
    @JoinColumn(name = "id_customer")
    Customer customer;

}
