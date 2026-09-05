package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.*;
import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.enums.DeviceStatusEnum;
import com.tproject.workshop.exception.BadRequestException;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.*;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final CustomerRepository customerRepository;
    private final ColorService colorService;
    private final TypesBrandsModelsService typeBrandModelService;
    private final TechnicianService technicianService;

    private static final List<DeviceStatusEnum> STATUS_THAT_RESET_URGENCY_REVISION = List.of(
            DeviceStatusEnum.PRONTO,
            DeviceStatusEnum.APROVADO,
            DeviceStatusEnum.NAO_APROVADO,
            DeviceStatusEnum.ENTREGUE,
            DeviceStatusEnum.DESCARTADO
    );

    private static final List<DeviceStatusEnum> STATUS_THAT_SET_DEPARTURE_DATE = List.of(
            DeviceStatusEnum.ENTREGUE, DeviceStatusEnum.DESCARTADO
    );

    private Device fetchOrThrow(int deviceId) {
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException(
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
        if (STATUS_THAT_SET_DEPARTURE_DATE.contains(newStatus)) {
            device.setDepartureDate(Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    private void revertStatusIfNeeded(Device device) {
        DeviceStatusEnum current = device.getDeviceStatus();
        if (current == DeviceStatusEnum.ENTREGUE || current == DeviceStatusEnum.DESCARTADO) {
            addHistory(device, DeviceHistoryFieldEnum.STATUS.getField(),
                    current.name(), DeviceStatusEnum.NOVO.name());
            device.setDeviceStatus(DeviceStatusEnum.NOVO);
        }
    }

    private Device changeStatus(int deviceId, DeviceStatusEnum newStatus) {
        Device device = fetchOrThrow(deviceId);
        DeviceStatusEnum oldStatus = device.getDeviceStatus();
        if (oldStatus != newStatus) {
            addHistory(device, DeviceHistoryFieldEnum.STATUS.getField(),
                    oldStatus.name(), newStatus.name());
        }
        device.setDeviceStatus(newStatus);
        resetUrgencyAndRevisionIfNeeded(device, newStatus);
        setDepartureDateIfDelivered(device, newStatus);
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
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Aparelho com id %d não encontrado", deviceId)));

    }

    @Transactional
    public void updateDeviceStatus(int deviceId, DeviceStatusEnum status) {
        deviceRepository.save(changeStatus(deviceId, status));
    }

    @Transactional
    public DeviceOutputDto updateDeviceStatus(int deviceId, DeviceStatusInputRecord dto) {
        DeviceStatusEnum newStatus = DeviceStatusEnum.fromString(dto.deviceStatus());
        deviceRepository.saveAndFlush(changeStatus(deviceId, newStatus));
        return findDeviceByIdOrThrow(deviceId);
    }

    @Transactional
    public DeviceOutputDto updateDeviceUrgency(int deviceId, DeviceUrgencyInputRecord dto) {
        Device device = fetchOrThrow(deviceId);
        boolean oldUrgency = device.isUrgency();
        boolean newUrgency = dto.hasUrgency();

        if (newUrgency && !oldUrgency) {
            revertStatusIfNeeded(device);
        }

        if (oldUrgency != newUrgency) {
            addHistory(device, DeviceHistoryFieldEnum.URGENCY.getField(),
                    String.valueOf(oldUrgency), String.valueOf(newUrgency));
        }

        device.setUrgency(newUrgency);
        deviceRepository.saveAndFlush(device);
        return findDeviceByIdOrThrow(device.getId());
    }

    @Transactional
    public DeviceOutputDto updateDeviceRevision(int deviceId, DeviceRevisionInputRecord dto) {
        Device device = fetchOrThrow(deviceId);
        boolean oldRevision = device.isRevision();
        boolean newRevision = dto.revision();

        if (newRevision && !oldRevision) {
            revertStatusIfNeeded(device);
        }

        if (oldRevision != newRevision) {
            addHistory(device, DeviceHistoryFieldEnum.REVISION.getField(),
                    String.valueOf(oldRevision), String.valueOf(newRevision));
        }

        device.setRevision(newRevision);
        deviceRepository.saveAndFlush(device);
        return findDeviceByIdOrThrow(device.getId());
    }

    @Transactional
    public DeviceOutputDto updateDevice(DeviceUpdateInputDtoRecord device) {
        Device oldDevice = fetchOrThrow(device.deviceId());

        DeviceStatusEnum newStatus = DeviceStatusEnum.fromString(device.deviceStatus());

        addHistories(oldDevice, device);

        oldDevice.setProblem(device.problem());
        oldDevice.setObservation(device.observation());
        oldDevice.setBudget(device.budget());
        oldDevice.setBudgetFee(device.budgetFee());
        oldDevice.setServiceValue(device.serviceValue());
        oldDevice.setUrgency(device.hasUrgency());
        oldDevice.setRevision(device.revision());
        oldDevice.setDeviceStatus(newStatus);
        Optional.ofNullable(device.technicianId()).ifPresent(id -> {
            Technician technician = technicianService.findById(id);
            oldDevice.setTechnician(technician);
        });

        if (device.entryDate() != null && device.departureDate() != null
                && !device.departureDate().isAfter(device.entryDate())) {
            throw new BadRequestException(
                    "A data de saída (%s) deve ser posterior à data de entrada (%s)",
                    device.departureDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    device.entryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        if (device.entryDate() != null) {
            oldDevice.setEntryDate(Timestamp.valueOf(device.entryDate()));
        }

        if (device.departureDate() != null) {
            oldDevice.setDepartureDate(Timestamp.valueOf(device.departureDate()));
        } else if (STATUS_THAT_SET_DEPARTURE_DATE.contains(newStatus)) {
            oldDevice.setDepartureDate(Timestamp.valueOf(LocalDateTime.now()));
        }

        deviceRepository.saveAndFlush(oldDevice);
        return findDeviceByIdOrThrow(oldDevice.getId());
    }

    @Transactional
    public CreateDeviceOutputDtoRecord createDevice(DeviceInputDtoRecord device) {
        Customer customer = customerRepository.findById(device.customerId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Cliente com id %d não encontrado", device.customerId())));

        BrandsModelsTypes brandModelType = typeBrandModelService.createOrReturnExistentBrandModelType(device.typeBrandModel());

        List<Color> colors = device.colors().stream().map(colorService::createOrReturnExistentColor).toList();
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
        Optional.ofNullable(device.technicianId()).ifPresent(id -> {
            Technician technician = technicianService.findById(id);
            newDevice.setTechnician(technician);
        });
        deviceHistoryList.add(new DeviceHistory(
                DeviceHistoryFieldEnum.STATUS.getField(), "", DeviceStatusEnum.NOVO.name(), newDevice));
        if (device.hasUrgency()) {
            deviceHistoryList.add(new DeviceHistory(
                    DeviceHistoryFieldEnum.URGENCY.getField(), "", "true", newDevice));
        }
        newDevice.setDeviceHistory(deviceHistoryList);

        Device savedDevice = deviceRepository.saveAndFlush(newDevice);
        return new CreateDeviceOutputDtoRecord(savedDevice.getId());
    }

    private void addHistories(Device oldDevice, DeviceUpdateInputDtoRecord newDevice) {
        if (oldDevice.isRevision() != newDevice.revision()) {
            addHistory(oldDevice, DeviceHistoryFieldEnum.REVISION.getField(),
                    String.valueOf(oldDevice.isRevision()), String.valueOf(newDevice.revision()));
        }
        if (oldDevice.isUrgency() != newDevice.hasUrgency()) {
            addHistory(oldDevice, DeviceHistoryFieldEnum.URGENCY.getField(),
                    String.valueOf(oldDevice.isUrgency()), String.valueOf(newDevice.hasUrgency()));
        }
        if (!oldDevice.getDeviceStatus().name().equalsIgnoreCase(newDevice.deviceStatus())) {
            addHistory(oldDevice, DeviceHistoryFieldEnum.STATUS.getField(),
                    oldDevice.getDeviceStatus().name(), newDevice.deviceStatus());
        }
    }
}
