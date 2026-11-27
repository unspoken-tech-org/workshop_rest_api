package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * Backward compatible facade of {@code /v1/device/*} (section 4.3 of
 * PLANO_SEPARACAO_APARELHO_OS.md). Not transactional: each write runs in {@link
 * ServiceOrderCommands} and commits before {@link ServiceOrderQueries} reads the result.
 */
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final ServiceOrderCommands commands;
    private final ServiceOrderQueries queries;

    public Page<DeviceTableDto> listDevices(DeviceQueryParam deviceQueryParam) {
        return queries.list(deviceQueryParam);
    }

    public Page<DeviceTableDto> searchDevices(DeviceSearchParam params) {
        return queries.search(params);
    }

    public DeviceOutputDto findDeviceByIdOrThrow(int deviceId) {
        return queries.findByIdOrThrow(deviceId);
    }

    public DeviceOutputDto updateDeviceStatus(int deviceId, DeviceStatusInputRecord dto) {
        commands.changeStatus(deviceId, dto.deviceStatus(), "status");
        return queries.findByIdOrThrow(deviceId);
    }

    public DeviceOutputDto updateDeviceUrgency(int deviceId, DeviceUrgencyInputRecord dto) {
        commands.markUrgent(deviceId, dto.hasUrgency());
        return queries.findByIdOrThrow(deviceId);
    }

    public DeviceOutputDto updateDeviceRevision(int deviceId, DeviceRevisionInputRecord dto) {
        commands.markRevision(deviceId, dto.revision());
        return queries.findByIdOrThrow(deviceId);
    }

    public DeviceOutputDto updateDevice(DeviceUpdateInputDtoRecord device) {
        commands.update(device);
        return queries.findByIdOrThrow(device.deviceId());
    }

    public CreateDeviceOutputDtoRecord createDevice(DeviceInputDtoRecord device) {
        return new CreateDeviceOutputDtoRecord(commands.create(device));
    }
}
