package com.tproject.workshop.controller;

import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;

import com.tproject.workshop.dto.device.DeviceUpdateInputDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface DeviceController {

    @PostMapping("/filter")
    List<DeviceTableDto> list(DeviceQueryParam deviceQueryParam);

    @GetMapping("/{id}")
    DeviceOutputDto findOne(@PathVariable("id") int deviceId);

    @PutMapping("/update")
    DeviceOutputDto update(@RequestBody DeviceUpdateInputDto device);
}
