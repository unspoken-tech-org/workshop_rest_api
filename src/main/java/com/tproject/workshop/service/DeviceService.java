package com.tproject.workshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.device.DeviceInputDto;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final CustomerService customerService;
    private final DeviceStatusService deviceStatusService;
    private final TechnicianService technicianService;

    private final BrandsModelsAndTypesService brandsModelsAndTypesService;

    private final ObjectMapper mapper;


    public DeviceOutputDto save(DeviceInputDto deviceInputDto){
        var newDevice =  mapper.convertValue(deviceInputDto, Device.class);

        newDevice.setCustomer(customerService.findById(deviceInputDto.getCustomerId()));
        newDevice.setTechnician(technicianService.findById(deviceInputDto.getTechnicianId()));
        newDevice.setDeviceStatus(deviceStatusService.findById(deviceInputDto.getDeviceStatusId()));
        newDevice.setBrandsModelsTypes(brandsModelsAndTypesService.findOrCreateBrandModelType(deviceInputDto.getBrandId(), deviceInputDto.getModelId(), deviceInputDto.getTypeId()));

        var savedDevice = deviceRepository.save(newDevice);
        return new DeviceOutputDto(savedDevice.getId(),
                savedDevice.getCustomer().getIdCustomer(),
                savedDevice.getDeviceStatus().getId(),
                savedDevice.getBrandsModelsTypes().getIdBrandModel().getIdBrand().getIdBrand(),
                savedDevice.getBrandsModelsTypes().getIdBrandModel().getIdModel().getIdModel(),
                savedDevice.getBrandsModelsTypes().getIdType().getIdType(),
                savedDevice.getTechnician().getId(), savedDevice.getProblem(),
                savedDevice.getObservation(), savedDevice.isHasUrgency());
    }

    public List<DeviceTableDto> listTable(DeviceQueryParam params) {
        return deviceRepository.listTable(params);
    }
}
