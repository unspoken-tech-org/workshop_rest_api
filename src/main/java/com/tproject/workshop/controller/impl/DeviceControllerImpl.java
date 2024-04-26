package com.tproject.workshop.controller.impl;

import com.tproject.workshop.controller.DeviceController;
import com.tproject.workshop.dto.device.DeviceInputDto;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.model.Device;
import com.tproject.workshop.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/device")
public class DeviceControllerImpl  implements DeviceController {

    private final DeviceService deviceService;

    @Override
    public List<DeviceTableDto> findAllTable(DeviceQueryParam params) {
        return deviceService.listTable(params);
    }

    @Override
    public Device findById(int id) {
        return null;
    }

    @Override
    public DeviceOutputDto save(@RequestBody DeviceInputDto device) {
        return deviceService.save(device);
    }
}
