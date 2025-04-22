package com.tproject.workshop.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.sql.Timestamp;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity(name = "payments")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private final Integer id;

  @CreationTimestamp
  @Column(name = "payment_date", updatable = false)
  private final Timestamp paymentDate;

  @Column(name = "payment_type", updatable = false)
  private final String paymentType;

  @Column(name = "payment_value", updatable = false)
  private final BigDecimal paymentValue;

  @Column(name = "category", updatable = false)
  private final String category;

  @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @JoinColumn(name = "id_device")
  private Device device;
}
