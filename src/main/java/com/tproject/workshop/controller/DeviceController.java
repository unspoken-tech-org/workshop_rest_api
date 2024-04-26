package com.tproject.workshop.controller;


import com.tproject.workshop.dto.device.DeviceInputDto;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.model.Device;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

public interface DeviceController {

    @GetMapping("/table")
    List<DeviceTableDto> findAllTable(DeviceQueryParam params);

    @GetMapping("/{id}")
    Device findById(int id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DeviceOutputDto save(DeviceInputDto device);
}
