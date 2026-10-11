package com.tproject.workshop.repository.jdbc;


import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;

public interface DeviceStatisticsRepository {

    DeviceStatisticsOutputDtoRecord getDeviceStatistics();
}
