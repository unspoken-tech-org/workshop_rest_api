package com.tproject.workshop.controller;

import com.tproject.workshop.dto.device.DeviceTableDto;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface DeviceController {

    @GetMapping
    List<DeviceTableDto> list();
}
