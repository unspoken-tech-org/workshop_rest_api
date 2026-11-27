package com.tproject.workshop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.ServiceOrderConflictException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
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
    @Setter(AccessLevel.NONE)
    private Timestamp entryDate;

    @Column(name = "departure_date")
    @Setter(AccessLevel.NONE)
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
    @Setter(AccessLevel.NONE)
    private boolean urgency;

    @Column(name = "is_revision", nullable = false)
    @Setter(AccessLevel.NONE)
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
    @Setter(AccessLevel.NONE)
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
    @Setter(AccessLevel.NONE)
    private List<ServiceOrderHistory> deviceHistory = new ArrayList<>();

    /**
     * Physical device of this visit. Null only for rows inserted without it, which the database
     * trigger {@code trg_devices_fill_customer_device} completes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_customer_device")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private CustomerDevice customerDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_origin_order")
    @ToString.Exclude
    @Setter(AccessLevel.NONE)
    private ServiceOrder originOrder;

    @Column(name = "warranty_days")
    @Setter(AccessLevel.NONE)
    private Integer warrantyDays = DEFAULT_WARRANTY_DAYS;

    @Column(name = "public_token", nullable = false, updatable = false)
    private UUID publicToken = UUID.randomUUID();

    /** When the order last became ENTREGUE/DESCARTADO; drives the undo-close window. */
    @Column(name = "closed_at")
    @Setter(AccessLevel.NONE)
    private Timestamp closedAt;

    public static final int DEFAULT_WARRANTY_DAYS = 90;

    public ServiceOrder(int id) {
        this.id = id;
    }

    // ---------------------------------------------------------------------------------------
    // Domain behavior (decisions 2, 3, 6, 7 and 8 of PLANO_SEPARACAO_APARELHO_OS.md)
    // ---------------------------------------------------------------------------------------

    /**
     * Opens a new order for a physical device. With an origin order the visit is linked to it
     * (decision 7); a revision always starts as {@code is_revision = true}.
     */
    public static ServiceOrder open(
            CustomerDevice device,
            ServiceOrder origin,
            boolean revision,
            String problem,
            String observation,
            BigDecimal budgetFee,
            boolean urgency,
            Technician technician,
            Instant now) {
        ServiceOrder order = new ServiceOrder();
        order.assignCustomerDevice(device);
        order.originOrder = origin;
        order.problem = problem;
        order.observation = observation;
        order.budgetFee = Objects.requireNonNullElse(budgetFee, BigDecimal.ZERO);
        order.technician = technician;
        order.deviceStatus = DeviceStatusEnum.NOVO;
        order.entryDate = Timestamp.from(now);
        order.addHistory(DeviceHistoryFieldEnum.STATUS, "", DeviceStatusEnum.NOVO.name());
        if (urgency) {
            order.urgency = true;
            order.addHistory(DeviceHistoryFieldEnum.URGENCY, "", "true");
        }
        if (revision) {
            order.revision = true;
            order.addHistory(DeviceHistoryFieldEnum.REVISION, "", "true");
        }
        return order;
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

    /** Moves the order to another physical device of the same customer (device merge). */
    public void moveToCustomerDevice(CustomerDevice target) {
        if (customerDevice != null && customerDevice.getId() != target.getId()) {
            addHistory(
                    DeviceHistoryFieldEnum.CUSTOMER_DEVICE,
                    String.valueOf(customerDevice.getId()),
                    String.valueOf(target.getId()));
        }
        assignCustomerDevice(target);
    }

    public boolean isClosed() {
        return deviceStatus != null && deviceStatus.isClosed();
    }

    /**
     * Changes the status. A closed order (ENTREGUE/DESCARTADO) is final: only {@link #undoClose}
     * reverts it. Closing stamps the departure date (if still empty) and {@code closed_at}; the
     * statuses that reset urgency clear only the urgency, never the revision (decision 7).
     *
     * @return whether the status changed
     */
    public boolean changeStatus(DeviceStatusEnum newStatus, Instant now) {
        Objects.requireNonNull(newStatus, "newStatus");
        if (newStatus == deviceStatus) {
            return false;
        }
        ensureOpen();
        DeviceStatusEnum previous = deviceStatus;
        if (newStatus.resetsUrgency() && urgency) {
            addHistory(DeviceHistoryFieldEnum.URGENCY, "true", "false");
            urgency = false;
        }
        addHistory(DeviceHistoryFieldEnum.STATUS, previous.name(), newStatus.name());
        deviceStatus = newStatus;
        if (newStatus.isClosed()) {
            if (departureDate == null) {
                departureDate = Timestamp.from(now);
            }
            closedAt = Timestamp.from(now);
        }
        if (!newStatus.allowsWarranty()) {
            warrantyDays = null;
        }
        return true;
    }

    /**
     * @return whether the urgency changed
     */
    public boolean markUrgent(boolean newUrgency) {
        if (newUrgency == urgency) {
            return false;
        }
        ensureOpen();
        addHistory(
                DeviceHistoryFieldEnum.URGENCY,
                String.valueOf(urgency),
                String.valueOf(newUrgency));
        urgency = newUrgency;
        return true;
    }

    /**
     * @return whether the revision flag changed
     */
    public boolean markRevision(boolean newRevision) {
        if (newRevision == revision) {
            return false;
        }
        ensureOpen();
        addHistory(
                DeviceHistoryFieldEnum.REVISION,
                String.valueOf(revision),
                String.valueOf(newRevision));
        revision = newRevision;
        return true;
    }

    /**
     * Replaces the operational fields. On a closed order a request that repeats the persisted
     * values is accepted (the current app resends the whole object); any real change is a 409.
     */
    public void updateDetails(
            String newProblem,
            String newObservation,
            String newBudget,
            BigDecimal newBudgetFee,
            BigDecimal newServiceValue,
            Technician newTechnician) {
        BigDecimal effectiveFee = Objects.requireNonNullElse(newBudgetFee, BigDecimal.ZERO);
        boolean changed =
                !Objects.equals(problem, newProblem)
                        || !Objects.equals(observation, newObservation)
                        || !Objects.equals(budget, newBudget)
                        || !sameAmount(budgetFee, effectiveFee)
                        || !sameAmount(serviceValue, newServiceValue)
                        || (newTechnician != null && !sameTechnician(newTechnician));
        if (!changed) {
            return;
        }
        ensureOpen();
        problem = newProblem;
        observation = newObservation;
        budget = newBudget;
        budgetFee = effectiveFee;
        serviceValue = newServiceValue;
        if (newTechnician != null) {
            technician = newTechnician;
        }
    }

    /**
     * Sets the entry and departure dates. {@code null} keeps the persisted value. The departure
     * date is immutable once set (decision 6): a different value is a 400, the same value is
     * accepted.
     */
    public void changeDates(LocalDateTime newEntryDate, LocalDateTime newDepartureDate) {
        Timestamp entry = newEntryDate != null ? Timestamp.valueOf(newEntryDate) : this.entryDate;
        Timestamp departure = this.departureDate;
        if (newDepartureDate != null) {
            Timestamp requested = Timestamp.valueOf(newDepartureDate);
            if (departureDate != null && !departureDate.equals(requested)) {
                throw new BadRequestException(
                        "A data de saída já foi registrada (%s) e não pode ser alterada",
                        departureDate.toLocalDateTime().format(DATE_FORMAT));
            }
            departure = requested;
        }
        if (entry != null && departure != null && departure.before(entry)) {
            throw new BadRequestException(
                    "A data de saída (%s) deve ser posterior ou igual à data de entrada (%s)",
                    departure.toLocalDateTime().format(DATE_FORMAT),
                    entry.toLocalDateTime().format(DATE_FORMAT));
        }
        if (Objects.equals(entry, entryDate) && Objects.equals(departure, departureDate)) {
            return;
        }
        ensureOpen();
        entryDate = entry;
        departureDate = departure;
    }

    /**
     * Warranty in days, {@code null} for none. Editable only while the status allows it (decision
     * 2).
     */
    public void changeWarrantyDays(Integer newWarrantyDays) {
        if (Objects.equals(newWarrantyDays, warrantyDays)) {
            return;
        }
        ensureOpen();
        if (!deviceStatus.allowsWarrantyEdit()) {
            throw new BadRequestException(
                    "A garantia não pode ser alterada com a OS em %s", deviceStatus.name());
        }
        if (newWarrantyDays != null && newWarrantyDays < 0) {
            throw new BadRequestException("A garantia não pode ser negativa");
        }
        warrantyDays = newWarrantyDays;
    }

    /** Recomputes the budget fee flag from the fee payments already received. */
    public void refreshBudgetFeeCollected(BigDecimal totalPaidFee) {
        BigDecimal paid = Objects.requireNonNullElse(totalPaidFee, BigDecimal.ZERO);
        BigDecimal fee = Objects.requireNonNullElse(budgetFee, BigDecimal.ZERO);
        budgetFeeCollected = fee.signum() > 0 && paid.compareTo(fee) >= 0;
    }

    /**
     * Reverts the closing within {@code window} from {@code closed_at} (decision 8): back to the
     * status before the closing, departure date cleared, both changes in the history.
     */
    public void undoClose(Instant now, Duration window) {
        if (!isClosed()) {
            throw ServiceOrderConflictException.notClosed(id);
        }
        if (closedAt == null || closedAt.toInstant().plus(window).isBefore(now)) {
            throw ServiceOrderConflictException.undoCloseExpired(id);
        }
        DeviceStatusEnum previous = statusBeforeClosing();
        addHistory(DeviceHistoryFieldEnum.STATUS, deviceStatus.name(), previous.name());
        if (departureDate != null) {
            addHistory(
                    DeviceHistoryFieldEnum.DEPARTURE_DATE,
                    departureDate.toLocalDateTime().toString(),
                    "");
        }
        deviceStatus = previous;
        departureDate = null;
        closedAt = null;
        if (warrantyDays == null && previous.allowsWarranty()) {
            warrantyDays = DEFAULT_WARRANTY_DAYS;
        }
    }

    private DeviceStatusEnum statusBeforeClosing() {
        String current = deviceStatus.name();
        return deviceHistory.stream()
                .filter(h -> DeviceHistoryFieldEnum.STATUS.getField().equals(h.getFieldName()))
                .filter(h -> current.equals(h.getNewValue()))
                .max(
                        Comparator.comparing(
                                        ServiceOrderHistory::getHistoryDate,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(
                                        ServiceOrderHistory::getId,
                                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ServiceOrderHistory::getOldValue)
                .filter(old -> !old.isBlank())
                .map(DeviceStatusEnum::valueOf)
                .filter(old -> !old.isClosed())
                .orElseThrow(() -> ServiceOrderConflictException.undoCloseExpired(id));
    }

    private void ensureOpen() {
        if (isClosed()) {
            throw ServiceOrderConflictException.closed(id);
        }
    }

    private void addHistory(DeviceHistoryFieldEnum field, String oldValue, String newValue) {
        deviceHistory.add(new ServiceOrderHistory(field.getField(), oldValue, newValue, this));
    }

    private boolean sameTechnician(Technician other) {
        return technician != null && Objects.equals(technician.getId(), other.getId());
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
