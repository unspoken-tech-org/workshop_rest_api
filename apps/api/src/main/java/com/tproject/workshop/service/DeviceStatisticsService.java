package com.tproject.workshop.service;

import org.springframework.stereotype.Service;

import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;
import com.tproject.workshop.repository.jdbc.DeviceStatisticsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceStatisticsService {

    private final DeviceStatisticsRepository deviceStatisticsRepository;

    public DeviceStatisticsOutputDtoRecord getDeviceStatistics() {
        return deviceStatisticsRepository.getDeviceStatistics();
    }
}
