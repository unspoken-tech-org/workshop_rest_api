package com.tproject.workshop.controller;

import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;

public interface DeviceController {

    @PostMapping("/filter")
    List<DeviceTableDto> list(DeviceQueryParam deviceQueryParam);
}
