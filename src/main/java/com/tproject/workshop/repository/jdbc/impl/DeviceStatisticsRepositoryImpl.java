package com.tproject.workshop.repository.jdbc.impl;

import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.deviceStatistics.DeviceStatisticsOutputDtoRecord;
import com.tproject.workshop.repository.jdbc.DeviceStatisticsRepository;
import com.tproject.workshop.utils.UtilsSql;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DeviceStatisticsRepositoryImpl implements DeviceStatisticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public DeviceStatisticsOutputDtoRecord getDeviceStatistics() {
        try {
            String jsonResult = jdbcTemplate.queryForObject(
                    UtilsSql.getQuery("device/deviceStatistics"),
                    Map.of(),
                    String.class
            );

            return objectMapper.readValue(jsonResult, new TypeReference<DeviceStatisticsOutputDtoRecord>() {
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao processar o resultado JSON do banco de dados", e);
        }
    }
}
