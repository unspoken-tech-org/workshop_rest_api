package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.DeviceController;
import com.tproject.workshop.dto.device.*;
import com.tproject.workshop.events.DeviceViewedEvent;
import com.tproject.workshop.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/device")
public class DeviceControllerImpl implements DeviceController {

    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Page<DeviceTableDto> list(@RequestBody(required = false) @Valid DeviceQueryParam deviceQueryParam) {
        if (deviceQueryParam == null) {
            deviceQueryParam = new DeviceQueryParam();
        }
        return deviceService.listDevices(deviceQueryParam);
    }

    @Override
    public DeviceOutputDto findOne(int deviceId) {
        eventPublisher.publishEvent(new DeviceViewedEvent(this, deviceId));
        return deviceService.findDeviceById(deviceId);
    }

    @Override
    public DeviceOutputDto update(@Valid DeviceUpdateInputDtoRecord device) {
        return deviceService.updateDevice(device);
    }

    @Override
    public DeviceOutputDto updateStatus(int deviceId, @Valid DeviceStatusInputRecord dto) {
        return deviceService.updateDeviceStatus(deviceId, dto);
    }

    @Override
    public DeviceOutputDto updateUrgency(int deviceId, @Valid DeviceUrgencyInputRecord dto) {
        return deviceService.updateDeviceUrgency(deviceId, dto);
    }

    @Override
    public DeviceOutputDto updateRevision(int deviceId, @Valid DeviceRevisionInputRecord dto) {
        return deviceService.updateDeviceRevision(deviceId, dto);
    }

    @Override
    public CreateDeviceOutputDtoRecord create(@Valid @RequestBody DeviceInputDtoRecord device) {
        return deviceService.createDevice(device);
    }

    @Override
    public Page<DeviceTableDto> search(@RequestBody @Valid DeviceSearchParam params) {
        return deviceService.searchDevices(params);
    }
}
