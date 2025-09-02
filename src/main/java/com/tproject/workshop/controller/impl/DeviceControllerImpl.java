package com.tproject.workshop.controller.impl;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tproject.workshop.controller.DeviceController;
import com.tproject.workshop.dto.device.CreateDeviceOutputDtoRecord;
import com.tproject.workshop.dto.device.DeviceInputDtoRecord;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.dto.device.DeviceUpdateInputDtoRecord;
import com.tproject.workshop.service.DeviceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/device")
public class DeviceControllerImpl implements DeviceController {

    private final DeviceService deviceService;

    @Override
    public Page<DeviceTableDto> list(@RequestBody(required = false) @Valid DeviceQueryParam deviceQueryParam) {
        if (deviceQueryParam == null) {
            deviceQueryParam = new DeviceQueryParam();
        }
        return deviceService.listDevices(deviceQueryParam);
    }

    @Override
    public DeviceOutputDto findOne(int deviceId) {
        return deviceService.findDeviceById(deviceId);
    }

    @Override
    public DeviceOutputDto update(DeviceUpdateInputDtoRecord device) {
        return deviceService.updateDevice(device);
    }

    @Override
    public CreateDeviceOutputDtoRecord create(@Valid @RequestBody DeviceInputDtoRecord device) {
        return deviceService.createDevice(device);
    }
}
