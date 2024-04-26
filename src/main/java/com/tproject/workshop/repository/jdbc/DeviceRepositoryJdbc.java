package com.tproject.workshop.repository.jdbc;

import com.tproject.workshop.dto.device.DeviceInputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.model.Device;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface DeviceRepositoryJdbc {

    Device saveDevice(Device device);

    List<DeviceTableDto> listTable(DeviceQueryParam params);
}
