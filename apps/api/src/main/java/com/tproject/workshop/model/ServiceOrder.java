package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tproject.workshop.enums.DeviceStatusEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@FieldNameConstants(asEnum = true)
@Entity
@Table(name = "devices")
public class ServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private int id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

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

    @Column(name = "budget_fee", nullable = false)
    private BigDecimal budgetFee;

    @Column(name = "service_value")
    private BigDecimal serviceValue;

    @Column(name = "budget_fee_collected", nullable = false)
    private boolean budgetFeeCollected;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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
    private List<ServiceOrderHistory> deviceHistory;

    /**
     * Physical device of this visit. Null only for rows inserted without it, which the database
     * trigger {@code trg_devices_fill_customer_device} completes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer_device")
    @ToString.Exclude
    private CustomerDevice customerDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_origin_order")
    @ToString.Exclude
    private ServiceOrder originOrder;

    @Column(name = "warranty_days")
    private Integer warrantyDays = DEFAULT_WARRANTY_DAYS;

    @Column(name = "public_token", nullable = false, updatable = false)
    private UUID publicToken = UUID.randomUUID();

    /** When the order last became ENTREGUE/DESCARTADO; drives the undo-close window. */
    @Column(name = "closed_at")
    private Timestamp closedAt;

    public static final int DEFAULT_WARRANTY_DAYS = 90;

    public ServiceOrder(int id) {
        this.id = id;
    }

    /**
     * Links the physical device and copies its customer, type/brand/model and colors to the legacy
     * columns, which the previous API image still reads until the contraction migration.
     */
    public void assignCustomerDevice(CustomerDevice device) {
        this.customerDevice = device;
        this.customer = device.getCustomer();
        this.brandsModelsTypes = device.getBrandsModelsTypes();
        this.colorIds = device.getColorIds();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ServiceOrder that = (ServiceOrder) o;
        return getId() != 0 && getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
