package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.DeviceController;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/device")
public class DeviceControllerImpl implements DeviceController {

    private final DeviceService deviceService;

    @Override
    public List<DeviceTableDto> list() {
        return deviceService.list(new DeviceQueryParam());
    }
}
