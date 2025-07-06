package com.tproject.workshop.controller;

import com.tproject.workshop.dto.device.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface DeviceController {

    @PostMapping("/filter")
    List<DeviceTableDto> list(DeviceQueryParam deviceQueryParam);

    @GetMapping("/{deviceId}")
    DeviceOutputDto findOne(@PathVariable("deviceId") int deviceId);

    @PutMapping("/update")
    DeviceOutputDto update(@RequestBody DeviceUpdateInputDto device);

    @PostMapping("/create")
    @ResponseStatus(code = HttpStatus.CREATED)
    CreateDeviceOutputDtoRecord create(@RequestBody DeviceInputDtoRecord device);
}
