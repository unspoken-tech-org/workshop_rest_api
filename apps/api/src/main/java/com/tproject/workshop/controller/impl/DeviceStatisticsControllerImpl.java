package com.tproject.workshop.controller.impl;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tproject.workshop.controller.DeviceStatisticsController;
import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;
import com.tproject.workshop.service.DeviceStatisticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/device-statistics")
@RequiredArgsConstructor
public class DeviceStatisticsControllerImpl implements DeviceStatisticsController {

    private final DeviceStatisticsService deviceStatisticsService;

    @Override
    public DeviceStatisticsOutputDtoRecord getDeviceStatistics() {
        return deviceStatisticsService.getDeviceStatistics();
    }
}
