package com.tproject.workshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.dto.device.DeviceUpdateInputDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.model.DeviceStatus;
import com.tproject.workshop.repository.DeviceRepository;
import com.tproject.workshop.utils.MapUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceStatusService deviceStatusService;
    private final ObjectMapper mapper;

    public List<DeviceTableDto> list(DeviceQueryParam params) {
        return deviceRepository.listTable(params);
    }

    public DeviceOutputDto findDeviceById(int deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(() -> new NotFoundException(String.format("Aparelho com id %d não encontrado", deviceId)));
    }

    public DeviceOutputDto updateDevice(DeviceUpdateInputDto device) {
        Device oldDevice = deviceRepository.findById(device.getDeviceId())
                .orElseThrow(() -> new NotFoundException(String.format("Aparelho com id %d não encontrado", device.getDeviceId())));

        DeviceStatus newStatus = deviceStatusService.findByStatus(device.getDeviceStatus());

        BeanUtils.copyProperties(device, oldDevice, MapUtils.getNullPropertyNames(device));
        oldDevice.setDeviceStatus(newStatus);

        deviceRepository.save(oldDevice);
        deviceRepository.flush();

        return findDeviceById(oldDevice.getId());
    }
}
