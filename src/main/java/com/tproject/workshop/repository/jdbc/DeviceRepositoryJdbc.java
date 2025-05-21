package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;

import java.util.List;

public interface DeviceRepositoryJdbc {
    List<DeviceTableDto> listTable(DeviceQueryParam params);

    DeviceOutputDto findByDeviceId(int deviceId);
}
