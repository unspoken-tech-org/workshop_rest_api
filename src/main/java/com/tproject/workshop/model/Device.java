package com.tproject.workshop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@NoArgsConstructor
@FieldNameConstants(asEnum = true)
@Entity(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int id;

    @CreationTimestamp
    @Column(name = "entry_date", nullable = false)
    private Timestamp entryDate;

    @Column(name = "departure_date")
    private Timestamp departureDate;

    @Column(name = "problem", nullable = false)
    private String problem;

    @Column(name = "observation")
    private String observation;

    @Column(name = "budget")
    private String budget;

    @Column(name = "labor_value", nullable = false)
    private BigDecimal laborValue;

    @Column(name = "service_value")
    private BigDecimal serviceValue;

    @Column(name = "has_urgency", nullable = false)
    private boolean hasUrgency;

    @Column(name = "is_revision", nullable = false)
    private boolean revision;

    @Column(name = "color_ids", nullable = false)
    private List<Integer> colorIds;

    @UpdateTimestamp
    @Column(name = "last_update")
    private Timestamp lastUpdate;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer")
    private Customer customer;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_device_status")
    private DeviceStatus deviceStatus;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_technician")
    private Technician technician;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_brand_model_type")
    private BrandsModelsTypes brandsModelsTypes;

    public Device(int id) {
        this.id = id;
    }
}
