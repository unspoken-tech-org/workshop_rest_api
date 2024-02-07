package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.sql.Timestamp;

@Data
@Entity(name = "Vendas")
public class Sell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idvenda", updatable = false)
    int idSell;
    @Column(name = "nome", nullable = false)
    String name;
    @CreationTimestamp(source = SourceType.DB)
    @Column(name = "datacadastro", nullable = false)
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
    @JoinColumn(name = "idCustomer", referencedColumnName = "idCustomer")
    private Customer idCustomer;

}
