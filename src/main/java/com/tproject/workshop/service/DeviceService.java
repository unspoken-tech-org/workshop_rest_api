package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.*;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.*;
import com.tproject.workshop.repository.CustomerRepository;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.utils.MapUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusService deviceStatusService;
    private final CustomerRepository customerRepository;
    private final ColorService colorService;
    private final TypesBrandsModelsService typeBrandModelService;
    private final TechnicianService technicianService;

    public List<DeviceTableDto> list(DeviceQueryParam params) {
        return deviceRepository.listTable(params);
    }

    @Transactional(readOnly = true)
    public DeviceOutputDto findDeviceById(int deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new NotFoundException(String.format("Aparelho com id %d não encontrado", deviceId)));
    }

    public DeviceOutputDto updateDevice(DeviceUpdateInputDto device) {
        Device oldDevice = deviceRepository.findById(device.getDeviceId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Aparelho com id %d não encontrado", device.getDeviceId())));

        DeviceStatus newStatus = deviceStatusService.findByStatus(device.getDeviceStatus());

        BeanUtils.copyProperties(device, oldDevice, MapUtils.getNullPropertyNames(device));
        oldDevice.setDeviceStatus(newStatus);

        deviceRepository.saveAndFlush(oldDevice);

        return findDeviceById(oldDevice.getId());
    }

    @Transactional
    public CreateDeviceOutputDtoRecord createDevice(DeviceInputDtoRecord device) {
        Customer customer = customerRepository.findById(device.customerId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Cliente com id %d não encontrado", device.customerId())));

        Technician technician = null;
        if (device.technicianId() != null) {
            technician = technicianService.findById(device.technicianId());
        }

        BrandsModelsTypes brandModelType = typeBrandModelService.createOrReturnExistentBrandModelType(device.typeBrandModel());
        DeviceStatus deviceStatus = deviceStatusService.findByStatus("novo");

        List<Color> colors = device.colors().stream().map(colorService::createOrReturnExistentColor).toList();
        List<Integer> colorIds = colors.stream().map(Color::getIdColor).toList();


        Device newDevice = new Device();
        newDevice.setCustomer(customer);
        newDevice.setBrandsModelsTypes(brandModelType);
        newDevice.setColorIds(colorIds);
        newDevice.setProblem(device.problem());
        newDevice.setObservation(device.observation());
        newDevice.setLaborValue(device.budgetValue());
        newDevice.setHasUrgency(device.hasUrgency());
        newDevice.setDeviceStatus(deviceStatus);

        if (technician != null) {
            newDevice.setTechnician(technician);
        }

        Device savedDevice = deviceRepository.saveAndFlush(newDevice);

        return new CreateDeviceOutputDtoRecord(savedDevice.getId());
    }

}
