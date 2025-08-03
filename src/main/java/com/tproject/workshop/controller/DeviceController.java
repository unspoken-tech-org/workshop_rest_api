package com.tproject.workshop.controller;

import com.tproject.workshop.dto.device.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

public interface DeviceController {

    @PostMapping("/filter")
    Page<DeviceTableDto> list(DeviceQueryParam deviceQueryParam);

    @GetMapping("/{deviceId}")
    DeviceOutputDto findOne(@PathVariable("deviceId") int deviceId);

    @PutMapping("/update")
    DeviceOutputDto update(@RequestBody DeviceUpdateInputDtoRecord device);

    @PostMapping("/create")
    @ResponseStatus(code = HttpStatus.CREATED)
    CreateDeviceOutputDtoRecord create(@RequestBody DeviceInputDtoRecord device);
}
