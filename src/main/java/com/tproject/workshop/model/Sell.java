package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.sql.Timestamp;

@Data
@Entity(name = "sales")
public class Sell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idSell;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "saleDate", nullable = false)
    Timestamp insertDate;
    @Column(name = "invoiceType")
    String invoiceType;
    @Column(name = "invoiceNumber")
    String invoiceNumber;
    @Column(name = "total")
    float total;
    @Column(name = "paidTotal")
    float paidTotal;
    @Column(name = "sellType")
    String sellType;
    @ManyToOne
    @JoinColumn(name = "idCustomer")
    Customer customer;

}
