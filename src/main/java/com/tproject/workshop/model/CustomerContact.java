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
import java.sql.Timestamp;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Entity(name = "customer_contact")
public class CustomerContact {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false)
  private int id;

  @Column(name = "id_device", nullable = false)
  private int deviceId;

  @Column(name = "id_technician", nullable = false)
  private int technicianId;

  @Column(name = "id_phone")
  private Integer phoneId;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "has_made_contact")
  private boolean hasMadeContact;

  @CreationTimestamp
  @Column(name = "last_contact", nullable = false)
  private Timestamp lastContact;

  @Column(name = "conversation")
  private String conversation;

  @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
  @JoinColumn(name = "id_device_status")
  private DeviceStatus deviceStatus;
}
