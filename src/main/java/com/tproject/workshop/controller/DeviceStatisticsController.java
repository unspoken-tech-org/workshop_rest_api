package com.tproject.workshop.controller;

import org.springframework.web.bind.annotation.GetMapping;

import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;


public interface DeviceStatisticsController {


    @GetMapping
    public DeviceStatisticsOutputDtoRecord getDeviceStatistics();
}
