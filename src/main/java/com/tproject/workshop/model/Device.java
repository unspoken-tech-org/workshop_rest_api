package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tproject.workshop.enums.DeviceStatusEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
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

    @Column(name = "labor_value_collected", nullable = false)
    private boolean laborValueCollected;

    @Column(name = "has_urgency", nullable = false)
    private boolean urgency;

    @Column(name = "is_revision", nullable = false)
    private boolean revision;

    @Column(name = "color_ids", nullable = false)
    private List<Integer> colorIds;

    @UpdateTimestamp
    @Column(name = "last_update")
    private Timestamp lastUpdate;

    @Column(name = "last_viewed_at")
    private Timestamp lastViewedAt;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer")
    @ToString.Exclude
    private Customer customer;

    @JsonIgnore
    @Enumerated(EnumType.STRING)
    @Column(name = "device_status")
    private DeviceStatusEnum deviceStatus;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_technician")
    @ToString.Exclude
    private Technician technician;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_brand_model_type")
    @ToString.Exclude
    private BrandsModelsTypes brandsModelsTypes;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<DeviceHistory> deviceHistory;

    public Device(int id) {
        this.id = id;
    }
}
