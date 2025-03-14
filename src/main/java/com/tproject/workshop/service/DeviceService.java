package com.tproject.workshop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tproject.workshop.dto.device.DeviceOutputDto;
import com.tproject.workshop.dto.device.DeviceQueryParam;
import com.tproject.workshop.dto.device.DeviceTableDto;
import com.tproject.workshop.exception.NotFoundException;
import com.tproject.workshop.repository.DeviceRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper mapper;

    public List<DeviceTableDto> list(DeviceQueryParam params){
        return deviceRepository.listTable(params);
    }

    public DeviceOutputDto findDeviceById(int deviceId){
        var result =  deviceRepository.findByDeviceId(deviceId);
        return Optional.of(result)
            .orElseThrow(() -> new NotFoundException(String.format("Aparelho com id %d", deviceId)));
    }
}
