package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.DeviceInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.enums.PaymentCategoryEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.events.observability.TurnaroundBand;
import com.tproject.workshop.events.observability.ValueBand;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.exception.ServiceOrderConflictException;
import com.tproject.workshop.model.BrandsModelsTypes;
import com.tproject.workshop.model.Color;
import com.tproject.workshop.model.Customer;
import com.tproject.workshop.model.CustomerDevice;
import com.tproject.workshop.model.ServiceOrder;
import com.tproject.workshop.model.Technician;
import com.tproject.workshop.repository.CustomerDeviceRepository;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.repository.PaymentRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Write side of service orders (JPA). Loads the {@link ServiceOrder} aggregate, calls its domain
 * methods (which hold decisions 2, 3, 6, 7 and 8) and records the observability events.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceOrderCommands {

    private final DeviceRepository deviceRepository;
    private final CustomerDeviceRepository customerDeviceRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final ColorService colorService;
    private final TypesBrandsModelsService typeBrandModelService;
    private final TechnicianService technicianService;
    private final ObservabilityEventRecorder eventRecorder;
    private final Clock clock;

    /**
     * Opens an order. With {@code originOrderId} the new visit reuses the physical device of the
     * origin order (decision 7); otherwise a new physical device is registered.
     *
     * @return the id of the new order
     */
    public int create(DeviceInputDtoRecord dto) {
        Customer customer =
                customerRepository
                        .findById(dto.customerId())
                        .orElseThrow(() -> NotFoundException.of("Cliente", dto.customerId()));
        ServiceOrder origin =
                Optional.ofNullable(dto.originOrderId()).map(this::fetch).orElse(null);
        CustomerDevice device =
                origin != null
                        ? deviceOfOrigin(origin, customer)
                        : customerDeviceRepository.save(newCustomerDevice(customer, dto));
        Technician technician =
                Optional.ofNullable(dto.technicianId())
                        .map(technicianService::findById)
                        .orElse(null);

        ServiceOrder order =
                ServiceOrder.open(
                        device,
                        origin,
                        dto.revision(),
                        dto.problem(),
                        dto.observation(),
                        dto.budgetFee(),
                        dto.hasUrgency(),
                        technician,
                        clock.instant());
        ServiceOrder saved = deviceRepository.save(order);
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_CREATED,
                Map.of("device.urgency", dto.hasUrgency() ? "urgent" : "normal"));
        return saved.getId();
    }

    private CustomerDevice deviceOfOrigin(ServiceOrder origin, Customer customer) {
        CustomerDevice device = origin.getCustomerDevice();
        if (device.getCustomer().getIdCustomer() != customer.getIdCustomer()) {
            throw new BadRequestException(
                    "A OS de origem %d pertence a outro cliente", origin.getId());
        }
        // Any order of the device, not only the origin: an older ENTREGUE origin must not
        // reopen a device that a later order discarded (decision 3).
        if (deviceRepository.existsByCustomerDeviceIdAndDeviceStatus(
                device.getId(), DeviceStatusEnum.DESCARTADO)) {
            throw ServiceOrderConflictException.disposed(device.getId());
        }
        return device;
    }

    private CustomerDevice newCustomerDevice(Customer customer, DeviceInputDtoRecord dto) {
        BrandsModelsTypes brandModelType =
                typeBrandModelService.createOrReturnExistentBrandModelType(dto.typeBrandModel());
        List<Integer> colorIds =
                dto.colors().stream()
                        .map(colorService::createOrReturnExistentColor)
                        .map(Color::getIdColor)
                        .distinct()
                        .toList();
        return new CustomerDevice(customer, brandModelType, colorIds);
    }

    public void changeStatus(int orderId, DeviceStatusEnum newStatus, String origin) {
        ServiceOrder order = fetch(orderId);
        applyStatus(order, newStatus, origin);
    }

    public void markUrgent(int orderId, boolean urgency) {
        ServiceOrder order = fetch(orderId);
        if (order.markUrgent(urgency)) {
            recordUrgencyChange(urgency);
        }
    }

    public void markRevision(int orderId, boolean revision) {
        fetch(orderId).markRevision(revision);
    }

    /**
     * Legacy full update ({@code PUT /v1/device/update}). Details, dates and flags are applied
     * before the status, so closing in the same request still works and a closed order accepts a
     * request that repeats its persisted values. The statuses that reset urgency ignore the
     * requested urgency; the revision is kept as sent (decision 7).
     */
    public void update(DeviceUpdateInputDtoRecord dto) {
        ServiceOrder order = fetch(dto.deviceId());
        BigDecimal previousBudgetFee = order.getBudgetFee();
        BigDecimal previousServiceValue = order.getServiceValue();
        DeviceStatusEnum newStatus = dto.deviceStatus();
        Technician technician =
                Optional.ofNullable(dto.technicianId())
                        .map(technicianService::findById)
                        .orElse(null);

        order.updateDetails(
                dto.problem(),
                dto.observation(),
                dto.budget(),
                dto.budgetFee(),
                dto.serviceValue(),
                technician);
        order.changeDates(dto.entryDate(), dto.departureDate());
        boolean effectiveUrgency = !newStatus.resetsUrgency() && dto.hasUrgency();
        if (order.markUrgent(effectiveUrgency)) {
            recordUrgencyChange(effectiveUrgency);
        }
        order.markRevision(dto.revision());
        applyStatus(order, newStatus, "device_update");
        order.refreshBudgetFeeCollected(
                paymentRepository.sumPaymentValueByDeviceIdAndCategory(
                        order.getId(), PaymentCategoryEnum.TAXA_ORCAMENTO.getDbValue()));

        recordBudgetChange("budget_fee", previousBudgetFee, dto.budgetFee());
        recordBudgetChange("service_value", previousServiceValue, dto.serviceValue());
    }

    /** 404 when the order does not exist, 409 when it is closed (decision 9). */
    @Transactional(readOnly = true)
    public void ensureOpenForContact(int orderId) {
        ServiceOrder order = fetch(orderId);
        if (order.isClosed()) {
            throw ServiceOrderConflictException.closed(orderId);
        }
    }

    /** Status carried by the latest contact of the order (API #193). */
    public void applyContactStatus(int orderId, DeviceStatusEnum status) {
        applyStatus(fetch(orderId), status, "customer_contact");
    }

    @Transactional(readOnly = true)
    public void ensureExists(int orderId) {
        fetch(orderId);
    }

    private ServiceOrder fetch(int orderId) {
        return deviceRepository
                .findById(orderId)
                .orElseThrow(() -> NotFoundException.of("Aparelho", orderId));
    }

    private void applyStatus(ServiceOrder order, DeviceStatusEnum newStatus, String origin) {
        DeviceStatusEnum previous = order.getDeviceStatus();
        boolean previousUrgency = order.isUrgency();
        Instant now = clock.instant();
        if (!order.changeStatus(newStatus, now)) {
            return;
        }
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_STATUS_CHANGED,
                Map.of(
                        "status.previous", previous.name().toLowerCase(Locale.ROOT),
                        "status.new", newStatus.name().toLowerCase(Locale.ROOT),
                        "change.origin", origin));
        if (previousUrgency && !order.isUrgency()) {
            recordUrgencyChange(false);
        }
        if (newStatus.isClosed()) {
            Instant entry =
                    Optional.ofNullable(order.getEntryDate())
                            .map(Timestamp::toInstant)
                            .orElse(null);
            eventRecorder.record(
                    ObservabilityEventCatalog.DEVICE_CLOSED,
                    Map.of(
                            "status.new", newStatus.name().toLowerCase(Locale.ROOT),
                            "turnaround.band", TurnaroundBand.of(entry, now)));
        }
    }

    private void recordUrgencyChange(boolean current) {
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                Map.of("device.urgency", current ? "urgent" : "normal"));
    }

    /** Null and zero both mean "no amount"; only the new value's band is emitted. */
    private void recordBudgetChange(String field, BigDecimal previous, BigDecimal current) {
        BigDecimal before = Optional.ofNullable(previous).orElse(BigDecimal.ZERO);
        BigDecimal after = Optional.ofNullable(current).orElse(BigDecimal.ZERO);
        int comparison = after.compareTo(before);
        if (comparison == 0) {
            return;
        }
        String changeType;
        if (before.signum() == 0) {
            changeType = "set";
        } else if (after.signum() == 0) {
            changeType = "cleared";
        } else {
            changeType = comparison > 0 ? "increase" : "decrease";
        }
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_BUDGET_CHANGED,
                Map.of(
                        "budget.field", field,
                        "budget.change_type", changeType,
                        "budget.value_band", ValueBand.of(after)));
    }
}
