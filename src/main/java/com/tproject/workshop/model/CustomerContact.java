package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tproject.workshop.enums.DeviceStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Entity(name = "customer_contact")
public class CustomerContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Integer id;

    @Column(name = "id_device", nullable = false)
    private int deviceId;

    @Column(name = "id_technician", nullable = false)
    private int technicianId;

    @Column(name = "phone")
    private String phone;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "has_made_contact")
    private boolean hasMadeContact;

    @Column(name = "last_contact", nullable = false)
    private Timestamp lastContact;

    @Column(name = "conversation")
    private String conversation;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "device_status")
    private DeviceStatusEnum deviceStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
