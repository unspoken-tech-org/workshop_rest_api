package com.tproject.workshop.service;

import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;
import com.tproject.workshop.repository.jdbc.DeviceStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceStatisticsService {

    private final DeviceStatisticsRepository deviceStatisticsRepository;

    public DeviceStatisticsOutputDtoRecord getDeviceStatistics() {
        return deviceStatisticsRepository.getDeviceStatistics();
    }
}
