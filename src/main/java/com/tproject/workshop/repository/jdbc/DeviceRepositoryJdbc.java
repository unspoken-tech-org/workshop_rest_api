package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;

import java.util.List;
import java.util.Optional;

public interface DeviceRepositoryJdbc {
    List<DeviceTableDto> listTable(DeviceQueryParam params);

    Optional<DeviceOutputDto> findByDeviceId(int deviceId);
}
