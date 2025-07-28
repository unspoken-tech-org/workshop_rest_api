package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.*;
import com.tproject.workshop.enums.DeviceHistoryFieldEnum;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.*;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tproject.workshop.events.DeviceViewedEvent;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusService deviceStatusService;
    private final CustomerRepository customerRepository;
    private final ColorService colorService;
    private final TypesBrandsModelsService typeBrandModelService;
    private final TechnicianService technicianService;
    private final ApplicationEventPublisher eventPublisher;

    public List<DeviceTableDto> list(DeviceQueryParam params) {
        return deviceRepository.listTable(params);
    }

    @Transactional(readOnly = true)
    public DeviceOutputDto findDeviceById(int deviceId) {
        DeviceOutputDto device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new NotFoundException(String.format("Aparelho com id %d não encontrado", deviceId)));

        eventPublisher.publishEvent(new DeviceViewedEvent(this, deviceId));

        return device;
    }

    public DeviceOutputDto updateDevice(DeviceUpdateInputDtoRecord device) {
        Device oldDevice = deviceRepository.findById(device.deviceId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Aparelho com id %d não encontrado", device.deviceId())));

        DeviceStatus newStatus = deviceStatusService.findByStatus(device.deviceStatus());
        Optional<DeviceHistory> optionalHistory = this.addDeviceHistoryOnUpdate(oldDevice, device);

        oldDevice.setProblem(device.problem());
        oldDevice.setObservation(device.observation());
        oldDevice.setBudget(device.budget());
        oldDevice.setLaborValue(device.laborValue());
        oldDevice.setServiceValue(device.serviceValue());
        oldDevice.setLaborValueCollected(device.laborValueCollected());
        oldDevice.setUrgency(device.hasUrgency());
        oldDevice.setRevision(device.revision());
        oldDevice.setDeviceStatus(newStatus);
        Optional.ofNullable(device.technicianId()).ifPresent(id -> {
            Technician technician = technicianService.findById(id);
            oldDevice.setTechnician(technician);
        });
        optionalHistory.ifPresent(history -> oldDevice.getDeviceHistory().add(history));

        deviceRepository.saveAndFlush(oldDevice);

        return findDeviceById(oldDevice.getId());
    }

    @Transactional
    public CreateDeviceOutputDtoRecord createDevice(DeviceInputDtoRecord device) {
        Customer customer = customerRepository.findById(device.customerId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Cliente com id %d não encontrado", device.customerId())));

        BrandsModelsTypes brandModelType = typeBrandModelService.createOrReturnExistentBrandModelType(device.typeBrandModel());
        DeviceStatus deviceStatus = deviceStatusService.findByStatus("novo");

        List<Color> colors = device.colors().stream().map(colorService::createOrReturnExistentColor).toList();
        List<Integer> colorIds = colors.stream().map(Color::getIdColor).toList();

        
        List<DeviceHistory> deviceHistoryList = List.of();

        Device newDevice = new Device();
        newDevice.setCustomer(customer);
        newDevice.setBrandsModelsTypes(brandModelType);
        newDevice.setColorIds(colorIds);
        newDevice.setProblem(device.problem());
        newDevice.setObservation(device.observation());
        newDevice.setLaborValue(device.budgetValue());
        newDevice.setUrgency(device.hasUrgency());
        newDevice.setDeviceStatus(deviceStatus);
        Optional.ofNullable(device.technicianId()).ifPresent(id -> {
            Technician technician = technicianService.findById(id);
            newDevice.setTechnician(technician);
        });
        deviceHistoryList.add(new DeviceHistory(DeviceHistoryFieldEnum.STATUS.getField(), "", "novo", newDevice));
        if (device.hasUrgency()) {
            deviceHistoryList.add(new DeviceHistory(DeviceHistoryFieldEnum.URGENCY.getField(), "", "true", newDevice));
        }
        newDevice.setDeviceHistory(deviceHistoryList);

        Device savedDevice = deviceRepository.saveAndFlush(newDevice);

        return new CreateDeviceOutputDtoRecord(savedDevice.getId());
    }

    Optional<DeviceHistory> addDeviceHistoryOnUpdate(Device oldDevice, DeviceUpdateInputDtoRecord newDevice) {
        boolean oldRevision = oldDevice.isRevision();
        boolean newRevision = newDevice.revision();
        boolean hasRevisionChanged = !(oldRevision == newRevision);


        boolean oldUrgency = oldDevice.isUrgency();
        boolean newUrgency = newDevice.hasUrgency();
        boolean hasUrgencyChanged = !(oldUrgency == newUrgency);

        String oldDeviceStatus = oldDevice.getDeviceStatus().getStatus();
        String newDeviceStatus = newDevice.deviceStatus();
        boolean hasDeviceStatusChanged = !(oldDeviceStatus.equals(newDeviceStatus));

        if (hasRevisionChanged || hasUrgencyChanged || hasDeviceStatusChanged) {
            DeviceHistory history = new DeviceHistory();
            history.setDevice(oldDevice);

            if (hasRevisionChanged) {
                history.setFieldName(DeviceHistoryFieldEnum.REVISION.getField());
                history.setOldValue(String.valueOf(oldRevision));
                history.setNewValue(String.valueOf(newRevision));
            } else if (hasUrgencyChanged) {
                history.setFieldName(DeviceHistoryFieldEnum.URGENCY.getField());
                history.setOldValue(String.valueOf(oldUrgency));
                history.setNewValue(String.valueOf(newUrgency));
            } else {
                history.setFieldName(DeviceHistoryFieldEnum.STATUS.getField());
                history.setOldValue(oldDeviceStatus);
                history.setNewValue(newDeviceStatus);
            }
            return Optional.of(history);
        }

        return Optional.empty();
    }

}
