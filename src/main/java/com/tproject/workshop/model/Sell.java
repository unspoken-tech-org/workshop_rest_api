package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.sql.Timestamp;

@Data
@Entity(name = "vendas")
public class Sell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    int idSell;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "datavenda", nullable = false)
    Timestamp insertDate;
    @Column(name = "tipocomprovante")
    String receipt;
    @Column(name = "numerocomprovante")
    String receiptNumber;
    @Column(name = "total")
    float total;
    @Column(name = "totalpago")
    float totalPay;
    @Column(name = "tipovenda")
    String sellType;
    @ManyToOne
    @JoinColumn(name = "idcliente")
    Customer customer;

}
