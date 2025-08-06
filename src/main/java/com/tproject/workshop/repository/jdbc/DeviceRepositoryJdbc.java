package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import org.springframework.data.domain.Page;


import java.util.Optional;

public interface DeviceRepositoryJdbc {
    Page<DeviceTableDto> listTable(DeviceQueryParam params);

    Optional<DeviceOutputDto> findByDeviceId(int deviceId);
}
