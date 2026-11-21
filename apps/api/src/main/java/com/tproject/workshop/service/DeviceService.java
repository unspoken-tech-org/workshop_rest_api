package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.*;
import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.events.observability.ObservabilityEventCatalog;
import com.tproject.workshop.events.observability.ObservabilityEventRecorder;
import com.tproject.workshop.events.observability.TurnaroundBand;
import com.tproject.workshop.events.observability.ValueBand;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.*;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final CustomerRepository customerRepository;
    private final ColorService colorService;
    private final TypesBrandsModelsService typeBrandModelService;
    private final TechnicianService technicianService;
    private final ObservabilityEventRecorder eventRecorder;

    private static final List<DeviceStatusEnum> STATUS_THAT_RESET_URGENCY_REVISION =
            List.of(
                    DeviceStatusEnum.PRONTO,
                    DeviceStatusEnum.APROVADO,
                    DeviceStatusEnum.NAO_APROVADO,
                    DeviceStatusEnum.ENTREGUE,
                    DeviceStatusEnum.DESCARTADO);

    private static final List<DeviceStatusEnum> STATUS_THAT_SET_DEPARTURE_DATE =
            List.of(DeviceStatusEnum.ENTREGUE, DeviceStatusEnum.DESCARTADO);

    private Device fetchOrThrow(int deviceId) {
        return deviceRepository
                .findById(deviceId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Aparelho com id %d não encontrado", deviceId)));
    }

    private void addHistory(Device device, String field, String oldValue, String newValue) {
        device.getDeviceHistory().add(new DeviceHistory(field, oldValue, newValue, device));
    }

    private void resetUrgencyAndRevisionIfNeeded(Device device, DeviceStatusEnum newStatus) {
        if (STATUS_THAT_RESET_URGENCY_REVISION.contains(newStatus)) {
            device.setUrgency(false);
            device.setRevision(false);
        }
    }

    private void setDepartureDateIfDelivered(Device device, DeviceStatusEnum newStatus) {
        if (STATUS_THAT_SET_DEPARTURE_DATE.contains(newStatus)
                && device.getDepartureDate() == null) {
            device.setDepartureDate(Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    private void recordStatusChange(
            DeviceStatusEnum previous, DeviceStatusEnum current, String origin) {
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_STATUS_CHANGED,
                Map.of(
                        "status.previous", previous.name().toLowerCase(Locale.ROOT),
                        "status.new", current.name().toLowerCase(Locale.ROOT),
                        "change.origin", origin));
    }

    private void recordClosedIfNeeded(
            Device device, DeviceStatusEnum previous, DeviceStatusEnum current) {
        if (previous == current
                || (current != DeviceStatusEnum.ENTREGUE
                        && current != DeviceStatusEnum.DESCARTADO)) {
            return;
        }
        Instant entry =
                Optional.ofNullable(device.getEntryDate()).map(Timestamp::toInstant).orElse(null);
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_CLOSED,
                Map.of(
                        "status.new", current.name().toLowerCase(Locale.ROOT),
                        "turnaround.band", TurnaroundBand.of(entry, Instant.now())));
    }

    private void recordUrgencyChange(boolean previous, boolean current) {
        if (previous != current) {
            eventRecorder.record(
                    ObservabilityEventCatalog.DEVICE_URGENCY_CHANGED,
                    Map.of("device.urgency", current ? "urgent" : "normal"));
        }
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

    private void revertStatusIfNeeded(Device device, String origin) {
        DeviceStatusEnum current = device.getDeviceStatus();
        if (current == DeviceStatusEnum.ENTREGUE || current == DeviceStatusEnum.DESCARTADO) {
            addHistory(
                    device,
                    DeviceHistoryFieldEnum.STATUS.getField(),
                    current.name(),
                    DeviceStatusEnum.NOVO.name());
            device.setDeviceStatus(DeviceStatusEnum.NOVO);
            recordStatusChange(current, DeviceStatusEnum.NOVO, origin);
        }
    }

    private Device changeStatus(int deviceId, DeviceStatusEnum newStatus, String origin) {
        Device device = fetchOrThrow(deviceId);
        DeviceStatusEnum oldStatus = device.getDeviceStatus();
        if (oldStatus != newStatus) {
            addHistory(
                    device,
                    DeviceHistoryFieldEnum.STATUS.getField(),
                    oldStatus.name(),
                    newStatus.name());
            recordStatusChange(oldStatus, newStatus, origin);
        }
        device.setDeviceStatus(newStatus);
        resetUrgencyAndRevisionIfNeeded(device, newStatus);
        setDepartureDateIfDelivered(device, newStatus);
        recordClosedIfNeeded(device, oldStatus, newStatus);
        return device;
    }

    @Transactional(readOnly = true)
    public Page<DeviceTableDto> listDevices(DeviceQueryParam deviceQueryParam) {
        return deviceRepository.listTable(deviceQueryParam);
    }

    @Transactional(readOnly = true)
    public Page<DeviceTableDto> searchDevices(DeviceSearchParam params) {
        return deviceRepository.searchTable(params);
    }

    @Transactional(readOnly = true)
    public DeviceOutputDto findDeviceByIdOrThrow(int deviceId) {
        return deviceRepository
                .findByDeviceId(deviceId)
                .orElseThrow(() ->
                        new NotFoundException(
                                String.format("Aparelho com id %d não encontrado", deviceId)));
    }

    @Transactional(readOnly = true)
    public void ensureDeviceExists(int deviceId) {
        fetchOrThrow(deviceId);
    }

    @Transactional
    public void updateDeviceStatus(int deviceId, DeviceStatusEnum status) {
        deviceRepository.save(changeStatus(deviceId, status, "customer_contact"));
    }

    @Transactional
    public DeviceOutputDto updateDeviceStatus(int deviceId, DeviceStatusInputRecord dto) {
        DeviceStatusEnum newStatus = DeviceStatusEnum.fromString(dto.deviceStatus());
        deviceRepository.saveAndFlush(changeStatus(deviceId, newStatus, "status"));
        return findDeviceByIdOrThrow(deviceId);
    }

    @Transactional
    public DeviceOutputDto updateDeviceUrgency(int deviceId, DeviceUrgencyInputRecord dto) {
        Device device = fetchOrThrow(deviceId);
        boolean oldUrgency = device.isUrgency();
        boolean newUrgency = dto.hasUrgency();

        if (newUrgency && !oldUrgency) {
            revertStatusIfNeeded(device, "urgency");
        }

        if (oldUrgency != newUrgency) {
            addHistory(
                    device,
                    DeviceHistoryFieldEnum.URGENCY.getField(),
                    String.valueOf(oldUrgency),
                    String.valueOf(newUrgency));
        }

        device.setUrgency(newUrgency);
        deviceRepository.saveAndFlush(device);
        recordUrgencyChange(oldUrgency, newUrgency);
        return findDeviceByIdOrThrow(device.getId());
    }

    @Transactional
    public DeviceOutputDto updateDeviceRevision(int deviceId, DeviceRevisionInputRecord dto) {
        Device device = fetchOrThrow(deviceId);
        boolean oldRevision = device.isRevision();
        boolean newRevision = dto.revision();

        if (newRevision && !oldRevision) {
            revertStatusIfNeeded(device, "revision");
        }

        if (oldRevision != newRevision) {
            addHistory(
                    device,
                    DeviceHistoryFieldEnum.REVISION.getField(),
                    String.valueOf(oldRevision),
                    String.valueOf(newRevision));
        }

        device.setRevision(newRevision);
        deviceRepository.saveAndFlush(device);
        return findDeviceByIdOrThrow(device.getId());
    }

    @Transactional
    public DeviceOutputDto updateDevice(DeviceUpdateInputDtoRecord device) {
        Device oldDevice = fetchOrThrow(device.deviceId());

        DeviceStatusEnum newStatus = DeviceStatusEnum.fromString(device.deviceStatus());
        DeviceStatusEnum previousStatus = oldDevice.getDeviceStatus();
        boolean previousUrgency = oldDevice.isUrgency();
        BigDecimal previousBudgetFee = oldDevice.getBudgetFee();
        BigDecimal previousServiceValue = oldDevice.getServiceValue();

        addHistories(oldDevice, device);

        oldDevice.setProblem(device.problem());
        oldDevice.setObservation(device.observation());
        oldDevice.setBudget(device.budget());
        oldDevice.setBudgetFee(device.budgetFee());
        oldDevice.setServiceValue(device.serviceValue());
        oldDevice.setUrgency(device.hasUrgency());
        oldDevice.setRevision(device.revision());
        oldDevice.setDeviceStatus(newStatus);
        Optional.ofNullable(device.technicianId())
                .ifPresent(id -> {
                    Technician technician = technicianService.findById(id);
                    oldDevice.setTechnician(technician);
                });

        LocalDateTime effectiveEntryDate =
                Optional.ofNullable(device.entryDate())
                        .orElseGet(() ->
                                oldDevice.getEntryDate() != null
                                        ? oldDevice.getEntryDate().toLocalDateTime()
                                        : null);

        LocalDateTime effectiveDepartureDate;
        if (device.departureDate() != null) {
            effectiveDepartureDate = device.departureDate();
        } else if (oldDevice.getDepartureDate() != null) {
            effectiveDepartureDate = oldDevice.getDepartureDate().toLocalDateTime();
        } else if (STATUS_THAT_SET_DEPARTURE_DATE.contains(newStatus)) {
            effectiveDepartureDate = LocalDateTime.now();
        } else {
            effectiveDepartureDate = null;
        }

        if (effectiveEntryDate != null
                && effectiveDepartureDate != null
                && effectiveDepartureDate.isBefore(effectiveEntryDate)) {
            throw new BadRequestException(
                    "A data de saída (%s) deve ser posterior ou igual à data de entrada (%s)",
                    effectiveDepartureDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    effectiveEntryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        if (device.entryDate() != null) {
            oldDevice.setEntryDate(Timestamp.valueOf(device.entryDate()));
        }

        if (device.departureDate() != null) {
            oldDevice.setDepartureDate(Timestamp.valueOf(device.departureDate()));
        } else if (oldDevice.getDepartureDate() == null
                && STATUS_THAT_SET_DEPARTURE_DATE.contains(newStatus)) {
            oldDevice.setDepartureDate(Timestamp.valueOf(effectiveDepartureDate));
        }

        deviceRepository.saveAndFlush(oldDevice);
        if (previousStatus != newStatus) {
            recordStatusChange(previousStatus, newStatus, "device_update");
            recordClosedIfNeeded(oldDevice, previousStatus, newStatus);
        }
        recordBudgetChange("budget_fee", previousBudgetFee, device.budgetFee());
        recordBudgetChange("service_value", previousServiceValue, device.serviceValue());
        recordUrgencyChange(previousUrgency, device.hasUrgency());
        return findDeviceByIdOrThrow(oldDevice.getId());
    }

    @Transactional
    public CreateDeviceOutputDtoRecord createDevice(DeviceInputDtoRecord device) {
        Customer customer =
                customerRepository
                        .findById(device.customerId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        String.format(
                                                "Cliente com id %d não encontrado",
                                                device.customerId())));

        BrandsModelsTypes brandModelType =
                typeBrandModelService.createOrReturnExistentBrandModelType(device.typeBrandModel());

        List<Color> colors =
                device.colors().stream().map(colorService::createOrReturnExistentColor).toList();
        List<Integer> colorIds = colors.stream().map(Color::getIdColor).toList();

        List<DeviceHistory> deviceHistoryList = new ArrayList<>();

        Device newDevice = new Device();
        newDevice.setCustomer(customer);
        newDevice.setBrandsModelsTypes(brandModelType);
        newDevice.setColorIds(colorIds);
        newDevice.setProblem(device.problem());
        newDevice.setObservation(device.observation());
        newDevice.setBudgetFee(Optional.ofNullable(device.budgetFee()).orElse(BigDecimal.ZERO));
        newDevice.setUrgency(device.hasUrgency());
        newDevice.setDeviceStatus(DeviceStatusEnum.NOVO);
        newDevice.setEntryDate(Timestamp.valueOf(LocalDateTime.now()));
        Optional.ofNullable(device.technicianId())
                .ifPresent(id -> {
                    Technician technician = technicianService.findById(id);
                    newDevice.setTechnician(technician);
                });
        deviceHistoryList.add(
                new DeviceHistory(
                        DeviceHistoryFieldEnum.STATUS.getField(),
                        "",
                        DeviceStatusEnum.NOVO.name(),
                        newDevice));
        if (device.hasUrgency()) {
            deviceHistoryList.add(
                    new DeviceHistory(
                            DeviceHistoryFieldEnum.URGENCY.getField(), "", "true", newDevice));
        }
        newDevice.setDeviceHistory(deviceHistoryList);

        Device savedDevice = deviceRepository.saveAndFlush(newDevice);
        eventRecorder.record(
                ObservabilityEventCatalog.DEVICE_CREATED,
                Map.of("device.urgency", device.hasUrgency() ? "urgent" : "normal"));
        return new CreateDeviceOutputDtoRecord(savedDevice.getId());
    }

    private void addHistories(Device oldDevice, DeviceUpdateInputDtoRecord newDevice) {
        if (oldDevice.isRevision() != newDevice.revision()) {
            addHistory(
                    oldDevice,
                    DeviceHistoryFieldEnum.REVISION.getField(),
                    String.valueOf(oldDevice.isRevision()),
                    String.valueOf(newDevice.revision()));
        }
        if (oldDevice.isUrgency() != newDevice.hasUrgency()) {
            addHistory(
                    oldDevice,
                    DeviceHistoryFieldEnum.URGENCY.getField(),
                    String.valueOf(oldDevice.isUrgency()),
                    String.valueOf(newDevice.hasUrgency()));
        }
        if (!oldDevice.getDeviceStatus().name().equalsIgnoreCase(newDevice.deviceStatus())) {
            addHistory(
                    oldDevice,
                    DeviceHistoryFieldEnum.STATUS.getField(),
                    oldDevice.getDeviceStatus().name(),
                    newDevice.deviceStatus());
        }
    }
}
