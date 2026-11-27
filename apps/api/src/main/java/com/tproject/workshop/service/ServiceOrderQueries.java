package com.tproject.workshop.service;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceSearchParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.repository.jdbc.DeviceRepositoryJdbc;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * Read side of service orders (JDBC). Runs outside the write transaction, so it always sees what
 * {@link ServiceOrderCommands} already committed, without {@code saveAndFlush}.
 */
@Service
@RequiredArgsConstructor
public class ServiceOrderQueries {

    private final DeviceRepositoryJdbc deviceRepositoryJdbc;

    public Page<DeviceTableDto> list(DeviceQueryParam params) {
        return deviceRepositoryJdbc.listTable(params);
    }

    public Page<DeviceTableDto> search(DeviceSearchParam params) {
        return deviceRepositoryJdbc.searchTable(params);
    }

    public DeviceOutputDto findByIdOrThrow(int orderId) {
        return deviceRepositoryJdbc
                .findByDeviceId(orderId)
                .orElseThrow(() -> NotFoundException.of("Aparelho", orderId));
    }
}
